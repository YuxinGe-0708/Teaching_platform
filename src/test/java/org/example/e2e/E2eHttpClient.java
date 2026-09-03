package org.example.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class E2eHttpClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final String baseUrl;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    E2eHttpClient(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    Response get(String path) throws IOException {
        return request("GET", path, null, null);
    }

    Response get(String path, Map<String, String> headers) throws IOException {
        return request("GET", path, null, headers);
    }

    Response postJson(String path, Object body) throws IOException {
        return postJson(path, body, null);
    }

    Response postJson(String path, Object body, Map<String, String> headers) throws IOException {
        return request("POST", path, JSON.writeValueAsBytes(body), mergeHeaders(headers, "application/json"));
    }

    Response putJson(String path, Object body, Map<String, String> headers) throws IOException {
        return request("PUT", path, JSON.writeValueAsBytes(body), mergeHeaders(headers, "application/json"));
    }

    Response postForm(String path, Map<String, ?> body) throws IOException {
        return postForm(path, body, null);
    }

    Response postForm(String path, Map<String, ?> body, Map<String, String> headers) throws IOException {
        return request("POST", path, formBytes(body), mergeHeaders(headers, "application/x-www-form-urlencoded"));
    }

    Response putForm(String path, Map<String, ?> body, Map<String, String> headers) throws IOException {
        return request("PUT", path, formBytes(body), mergeHeaders(headers, "application/x-www-form-urlencoded"));
    }

    Response delete(String path, Map<String, String> headers) throws IOException {
        return request("DELETE", path, null, headers);
    }

    private Response request(String method, String path, byte[] body, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(resolve(path)).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        if (!cookies.isEmpty()) connection.setRequestProperty("Cookie", cookieHeader());
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (header.getValue() != null) connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            connection.getOutputStream().write(body);
        }

        int status = connection.getResponseCode();
        captureCookies(connection);
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String text = stream == null ? "" : read(stream);
        Object json = parseJson(text);
        connection.disconnect();
        return new Response(status, text, json);
    }

    private String resolve(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return path.startsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    }

    private Map<String, String> mergeHeaders(Map<String, String> headers, String contentType) {
        Map<String, String> result = new LinkedHashMap<>();
        if (headers != null) result.putAll(headers);
        result.putIfAbsent("Content-Type", contentType);
        result.putIfAbsent("Accept", "application/json");
        return result;
    }

    private byte[] formBytes(Map<String, ?> body) throws IOException {
        StringBuilder result = new StringBuilder();
        if (body != null) {
            for (Map.Entry<String, ?> entry : body.entrySet()) {
                if (result.length() > 0) result.append('&');
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append('=');
                result.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
            }
        }
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void captureCookies(HttpURLConnection connection) {
        for (Map.Entry<String, java.util.List<String>> entry : connection.getHeaderFields().entrySet()) {
            if (!"Set-Cookie".equalsIgnoreCase(entry.getKey()) || entry.getValue() == null) continue;
            for (String cookie : entry.getValue()) {
                String[] parts = cookie.split(";", 2)[0].split("=", 2);
                if (parts.length == 2) cookies.put(parts[0], parts[1]);
            }
        }
    }

    private String cookieHeader() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (result.length() > 0) result.append("; ");
            result.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.toString();
    }

    private Object parseJson(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return JSON.readValue(text, Object.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String read(InputStream stream) throws IOException {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    static final class Response {
        final int status;
        final String body;
        final Object json;

        Response(int status, String body, Object json) {
            this.status = status;
            this.body = body;
            this.json = json;
        }

        int apiCode() {
            if (json instanceof Map && ((Map<?, ?>) json).get("code") instanceof Number) {
                return ((Number) ((Map<?, ?>) json).get("code")).intValue();
            }
            return status;
        }

        Object value() {
            return json instanceof Map ? ((Map<?, ?>) json).get("data") : json;
        }

        Map<String, Object> data() {
            Object value = value();
            return value instanceof Map ? castMap(value) : new LinkedHashMap<>();
        }

        String summary() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("httpStatus", status);
            result.put("apiCode", apiCode());
            if (json instanceof Map) {
                Map<?, ?> root = (Map<?, ?>) json;
                result.put("message", root.get("message"));
                result.put("data", root.get("data"));
            } else {
                result.put("body", body == null ? "" : body.substring(0, Math.min(body.length(), 500)));
            }
            return result.toString();
        }

        private static Map<String, Object> castMap(Object value) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
    }
}

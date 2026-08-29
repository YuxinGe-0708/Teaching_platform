package org.example.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class E2eHttpClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final String baseUrl;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    E2eHttpClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    Response get(String path) throws IOException {
        return request("GET", path, null, null);
    }

    Response postForm(String path, Map<String, ?> data) throws IOException {
        return request("POST", path, formBody(data), "application/x-www-form-urlencoded; charset=UTF-8");
    }

    Response postJson(String path, Object payload) throws IOException {
        return request("POST", path, JSON.writeValueAsBytes(payload), "application/json; charset=UTF-8");
    }

    Response postMultipart(String path, Map<String, ?> fields, String fileField, Path file) throws IOException {
        String boundary = "----TeachingPlatformE2E" + System.currentTimeMillis();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            writeUtf8(output, "--" + boundary + "\r\n");
            writeUtf8(output, "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
            writeUtf8(output, String.valueOf(entry.getValue()) + "\r\n");
        }
        writeUtf8(output, "--" + boundary + "\r\n");
        writeUtf8(output, "Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + file.getFileName() + "\"\r\n");
        writeUtf8(output, "Content-Type: " + contentType(file) + "\r\n\r\n");
        output.write(Files.readAllBytes(file));
        writeUtf8(output, "\r\n--" + boundary + "--\r\n");
        return request("POST", path, output.toByteArray(), "multipart/form-data; boundary=" + boundary);
    }

    private Response request(String method, String path, byte[] body, String contentType) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(90000);
        connection.setUseCaches(false);
        if (!cookies.isEmpty()) connection.setRequestProperty("Cookie", cookieHeader());
        if (contentType != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", contentType);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
        }
        int status = connection.getResponseCode();
        collectCookies(connection);
        byte[] responseBytes = readBytes(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                headers.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        connection.disconnect();
        return new Response(status, headers, responseBytes);
    }

    private void collectCookies(HttpURLConnection connection) {
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            if (entry.getKey() == null || !"Set-Cookie".equalsIgnoreCase(entry.getKey()) || entry.getValue() == null) continue;
            for (String header : entry.getValue()) {
            int separator = header.indexOf(';');
            String cookie = separator >= 0 ? header.substring(0, separator) : header;
            int equals = cookie.indexOf('=');
            if (equals > 0) cookies.put(cookie.substring(0, equals).trim(), cookie.substring(equals + 1).trim());
            }
        }
    }

    private String cookieHeader() {
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : cookies.entrySet()) values.add(entry.getKey() + "=" + entry.getValue());
        return String.join("; ", values);
    }

    private static byte[] formBody(Map<String, ?> data) throws IOException {
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            pairs.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
        }
        return String.join("&", pairs).getBytes(StandardCharsets.UTF_8);
    }

    private static String contentType(Path file) {
        try {
            String detected = Files.probeContentType(file);
            if (detected != null) return detected;
        } catch (IOException ignored) {
        }
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }

    private static void writeUtf8(OutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] readBytes(InputStream input) throws IOException {
        if (input == null) return new byte[0];
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) >= 0) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    static final class Response {
        final int status;
        final Map<String, String> headers;
        final byte[] bytes;

        Response(int status, Map<String, String> headers, byte[] bytes) {
            this.status = status;
            this.headers = headers;
            this.bytes = bytes;
        }

        String body() {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        String location() {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("location".equalsIgnoreCase(entry.getKey())) return entry.getValue();
            }
            return "";
        }

        Map<String, Object> summary() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", status);
            result.put("location", location());
            result.put("bodyExcerpt", body().substring(0, Math.min(500, body().length())));
            result.put("contentLength", bytes.length);
            return result;
        }
    }
}

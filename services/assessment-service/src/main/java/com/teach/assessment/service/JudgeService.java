package com.teach.assessment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class JudgeService {

    @Value("${app.judge0.api-url:https://ce.judge0.com}")
    private String apiUrl;

    @Value("${app.judge0.api-key:}")
    private String apiKey;

    @Value("${app.judge0.api-host:}")
    private String apiHost;

    @Value("${app.judge0.timeout-ms:15000}")
    private int timeoutMs;

    @Value("${app.judge0.local-fallback:true}")
    private boolean localFallbackEnabled;

    private final ObjectMapper mapper = new ObjectMapper();
    private final LocalJudgeService localJudgeService;

    public JudgeService(LocalJudgeService localJudgeService) {
        this.localJudgeService = localJudgeService;
    }

    public static class JudgeResult {
        public String status;
        public int passedCases;
        public int totalCases;
        public double score;
        public double timeUsedMs;
        public double memoryUsedKb;
        public String errorMessage;
        public List<CaseResult> caseResults = new ArrayList<>();
        public boolean usedLocalJudge;
    }

    public static class CaseResult {
        public int caseIndex;
        public String status;
        public String input;
        public String expectedOutput;
        public String actualOutput;
        public double timeMs;
        public double memoryKb;
        public String message;
    }

    public JudgeResult judge(String code, String language, List<Map<String, String>> testCases) {
        JudgeResult result = new JudgeResult();
        result.totalCases = testCases != null ? testCases.size() : 0;

        if (testCases == null || testCases.isEmpty()) {
            result.status = "IE";
            result.errorMessage = "编程题没有配置测试用例，请联系教师补充期望输出。";
            return result;
        }

        Integer languageId = languageId(language);
        if (languageId == null) {
            result.status = "IE";
            result.errorMessage = "暂不支持该语言：" + language;
            return result;
        }

        try {
            return doCloudJudge(code, languageId, testCases);
        } catch (RestClientException e) {
            if (localFallbackEnabled) {
                JudgeResult localResult = localJudgeService.judge(code, language, testCases);
                localResult.usedLocalJudge = true;
                return localResult;
            }
            JudgeResult errorResult = new JudgeResult();
            errorResult.totalCases = testCases.size();
            errorResult.status = "IE";
            errorResult.errorMessage = "云端判题服务连接失败且本地评测未启用：" + e.getMessage();
            return errorResult;
        }
    }

    private JudgeResult doCloudJudge(String code, Integer languageId, List<Map<String, String>> testCases) {
        JudgeResult result = new JudgeResult();
        result.totalCases = testCases.size();

        int totalWeight = 0;
        int passedWeight = 0;
        long started = System.currentTimeMillis();
        for (int i = 0; i < testCases.size(); i++) {
            Map<String, String> tc = testCases.get(i);
            CaseResult caseResult = submitCase(code, languageId, tc, i + 1);
            result.caseResults.add(caseResult);
            result.timeUsedMs += caseResult.timeMs;
            result.memoryUsedKb = Math.max(result.memoryUsedKb, caseResult.memoryKb);
            int w = parseWeight(tc.get("weight"));
            totalWeight += w;
            if ("AC".equals(caseResult.status)) {
                result.passedCases++;
                passedWeight += w;
            }
            if ("IE".equals(caseResult.status)) {
                result.status = "IE";
                result.errorMessage = caseResult.message;
                result.score = 0;
                return result;
            }
        }

        if (result.timeUsedMs <= 0) {
            result.timeUsedMs = System.currentTimeMillis() - started;
        }
        result.score = totalWeight == 0 ? 0 : (double) passedWeight / totalWeight * 100;
        result.status = result.passedCases == result.totalCases ? "AC" : worstStatus(result.caseResults);
        return result;
    }

    private CaseResult submitCase(String code, Integer languageId, Map<String, String> testCase, int index) {
        CaseResult cr = new CaseResult();
        cr.caseIndex = index;
        cr.input = testCase.getOrDefault("input", "");
        cr.expectedOutput = testCase.getOrDefault("expectedOutput", "").trim();

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("source_code", code);
            body.put("language_id", languageId);
            body.put("stdin", cr.input);
            body.put("expected_output", cr.expectedOutput);
            body.put("cpu_time_limit", Math.max(2, timeoutMs / 1000));
            body.put("wall_time_limit", Math.max(3, timeoutMs / 1000 + 2));

            String url = UriComponentsBuilder.fromHttpUrl(normalizedApiUrl() + "/submissions")
                    .queryParam("base64_encoded", "false")
                    .queryParam("wait", "false")
                    .queryParam("fields", "stdout,stderr,compile_output,message,status,time,memory")
                    .toUriString();

            long start = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate().exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers()),
                    String.class
            );
            JsonNode submitted = mapper.readTree(response.getBody());
            JsonNode json = submitted.has("token")
                    ? pollSubmission(submitted.get("token").asText(), start)
                    : submitted;
            cr.timeMs = System.currentTimeMillis() - start;
            JsonNode statusNode = json.path("status");
            cr.status = mapStatus(statusNode.path("id").asInt(0));
            cr.message = statusNode.path("description").asText("");
            cr.actualOutput = firstText(json, "stdout", "stderr", "compile_output", "message");
            double remoteTime = json.path("time").asDouble(0D);
            if (remoteTime > 0) {
                cr.timeMs = remoteTime * 1000D;
            }
            cr.memoryKb = json.path("memory").asDouble(0D);
        } catch (RestClientException e) {
            throw e;
        } catch (Exception e) {
            cr.status = "IE";
            cr.message = "云端判题异常：" + e.getMessage();
            cr.actualOutput = cr.message;
        }

        return cr;
    }

    private JsonNode pollSubmission(String token, long start) throws Exception {
        String url = UriComponentsBuilder.fromHttpUrl(normalizedApiUrl() + "/submissions/" + token)
                .queryParam("base64_encoded", "false")
                .queryParam("fields", "stdout,stderr,compile_output,message,status,time,memory")
                .toUriString();
        while (System.currentTimeMillis() - start < timeoutMs) {
            ResponseEntity<String> response = restTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    String.class
            );
            JsonNode json = mapper.readTree(response.getBody());
            int statusId = json.path("status").path("id").asInt(0);
            if (statusId > 2) {
                return json;
            }
            Thread.sleep(500L);
        }
        ObjectNode timeout = mapper.createObjectNode();
        ObjectNode status = timeout.putObject("status");
        status.put("id", 5);
        status.put("description", "Time Limit Exceeded");
        timeout.put("message", "云端判题等待超时，请稍后重试。");
        return timeout;
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0 TeachingPlatform/1.0");
        String key = apiKey == null ? "" : apiKey.trim();
        if (!key.isEmpty()) {
            if (key.toLowerCase().startsWith("bearer ")) {
                headers.set(HttpHeaders.AUTHORIZATION, key);
            } else {
                headers.set("X-RapidAPI-Key", key);
            }
        }
        String host = apiHost == null ? "" : apiHost.trim();
        if (!host.isEmpty()) {
            headers.set("X-RapidAPI-Host", host);
        }
        return headers;
    }

    private String normalizedApiUrl() {
        String url = apiUrl == null || apiUrl.trim().isEmpty() ? "https://ce.judge0.com" : apiUrl.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private Integer languageId(String language) {
        if (language == null) return null;
        String value = language.trim().toLowerCase();
        if ("python".equals(value) || "py".equals(value)) return 71;
        if ("java".equals(value)) return 62;
        if ("c".equals(value) || "gcc".equals(value)) return 50;
        return null;
    }

    private String mapStatus(int id) {
        if (id == 3) return "AC";
        if (id == 4) return "WA";
        if (id == 5) return "TLE";
        if (id == 6) return "CE";
        if (id >= 7 && id <= 12) return "RE";
        if (id == 13) return "IE";
        return "IE";
    }

    static int parseWeight(String weightStr) {
        if (weightStr == null) return 1;
        try {
            int w = Integer.parseInt(weightStr.trim());
            return w > 0 ? w : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String worstStatus(List<CaseResult> cases) {
        for (CaseResult cr : cases) if ("CE".equals(cr.status)) return "CE";
        for (CaseResult cr : cases) if ("RE".equals(cr.status)) return "RE";
        for (CaseResult cr : cases) if ("TLE".equals(cr.status)) return "TLE";
        for (CaseResult cr : cases) if ("WA".equals(cr.status)) return "WA";
        return "IE";
    }

    private String firstText(JsonNode json, String... fields) {
        for (String field : fields) {
            JsonNode node = json.get(field);
            if (node != null && !node.isNull()) {
                String text = node.asText("");
                if (!text.trim().isEmpty()) return text;
            }
        }
        return "";
    }
}

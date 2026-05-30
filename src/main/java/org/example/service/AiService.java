package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Value("${app.ai.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:deepseek-chat}")
    private String model;

    private RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, List<Map<String, String>>> sessions = new HashMap<>();

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        log.info("AI Service initialized, model={}, url={}, keyConfigured={}", model, apiUrl, apiKey != null && !apiKey.trim().isEmpty());
    }

    public String chat(String sessionId, String courseName, String userMessage) {
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.isEmpty()) {
            return "AI 助手尚未配置 API Key。请在启动前设置环境变量 AI_API_KEY。";
        }

        List<Map<String, String>> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        if (history.isEmpty()) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", "你是在线教学平台的 AI 助教。当前课程是《" + courseName
                    + "》。请用中文回答，重点帮助学生理解课程知识、完成作业思路和排查学习问题。回答尽量简洁，不超过 500 字。");
            history.add(sysMsg);
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 1000);
            ArrayNode messages = body.putArray("messages");
            for (Map<String, String> message : history) {
                ObjectNode msg = messages.addObject();
                msg.put("role", message.get("role"));
                msg.put("content", message.get("content"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (key.toLowerCase().startsWith("bearer ")) {
                headers.set(HttpHeaders.AUTHORIZATION, key);
            } else {
                headers.setBearerAuth(key);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers),
                    String.class
            );

            JsonNode responseJson = mapper.readTree(response.getBody());
            JsonNode choices = responseJson.get("choices");
            if (choices != null && choices.size() > 0) {
                String reply = choices.get(0).get("message").get("content").asText();
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", reply);
                history.add(assistantMsg);
                return reply;
            }

            String errorMsg = responseJson.has("error") ? responseJson.get("error").toString() : "Unknown response";
            log.error("AI API error: {}", errorMsg);
            return "AI API 返回错误：" + errorMsg;
        } catch (HttpClientErrorException.Unauthorized e) {
            history.remove(history.size() - 1);
            log.warn("AI unauthorized: {}", e.getMessage());
            return "AI 鉴权失败：当前 API Key、模型名或接口地址不匹配。请检查 AI_API_KEY 是否有效，AI_API_URL 是否对应这个服务商，且不要多写空格；如果变量里已经带 Bearer，系统会自动兼容。";
        } catch (Exception e) {
            history.remove(history.size() - 1);
            log.error("AI call failed: {}", e.toString());
            return "AI 调用失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
}

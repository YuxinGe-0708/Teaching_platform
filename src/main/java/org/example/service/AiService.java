package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

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
        log.info("AI Service initialized, model={}, url={}", model, apiUrl);
    }

    public String chat(String sessionId, String courseName, String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "AI助手暂未配置，请联系管理员设置 API Key。";
        }

        log.info("AI chat: session={}, course={}, msgLen={}", sessionId, courseName, userMessage.length());

        List<Map<String, String>> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        if (history.isEmpty()) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", "你是一个在线教学平台的 AI 助教。当前学生正在学习课程《" + courseName + "》。"
                + "请用中文回答，语气友好、耐心，帮助学生解决课程相关问题。回答尽量简洁，不超过300字。");
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
            for (Map<String, String> m : history) {
                ObjectNode msg = messages.addObject();
                msg.put("role", m.get("role"));
                msg.put("content", m.get("content"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ResponseEntity<String> resp = restTemplate.exchange(
                apiUrl, HttpMethod.POST, new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);

            log.info("AI response status: {}", resp.getStatusCodeValue());

            JsonNode respJson = mapper.readTree(resp.getBody());
            JsonNode choices = respJson.get("choices");
            if (choices != null && choices.size() > 0) {
                String reply = choices.get(0).get("message").get("content").asText();
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", reply);
                history.add(assistantMsg);
                return reply;
            }

            String errorMsg = respJson.has("error") ? respJson.get("error").toString() : "Unknown";
            log.error("AI API error: {}", errorMsg);
            return "AI API 返回错误：" + errorMsg;

        } catch (Exception e) {
            log.error("AI call failed: {}", e.toString());
            history.remove(history.size() - 1);
            return "AI 调用失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
}

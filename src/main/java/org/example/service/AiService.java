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

    @Value("${app.ai.vision-model:${app.ai.model:deepseek-chat}}")
    private String visionModel;

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
            String reply = sendTextMessages(model, history);
            if (reply != null && !reply.trim().isEmpty()) {
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", reply);
                history.add(assistantMsg);
                return reply;
            }
            return "AI API 返回内容为空，请稍后重试。";
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

    public String summarizePdfText(String courseName, String resourceTitle, String text) {
        if (!hasKey()) {
            return "AI 助手尚未配置 API Key。请在启动前设置环境变量 AI_API_KEY。";
        }
        String source = text == null ? "" : text.trim();
        if (source.isEmpty()) {
            return "这个 PDF 暂时没有提取到可用于总结的文本内容。";
        }
        if (source.length() > 12000) {
            source = source.substring(0, 12000);
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", "你是在线教学平台的 AI 助教，请为学生生成清晰、结构化的课程资料笔记。"));
        messages.add(message("user", "课程：" + courseName + "\n资料：" + resourceTitle
                + "\n请根据下面 PDF 文本生成中文知识点概括，包含：核心概念、重点公式/步骤、易错点、复习建议。\n\n"
                + source));
        try {
            String reply = sendTextMessages(model, messages);
            return reply == null || reply.trim().isEmpty() ? "AI 没有返回有效笔记，请稍后重试。" : reply;
        } catch (Exception e) {
            log.error("PDF note generation failed: {}", e.toString());
            return "AI 笔记生成失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    public String generateMindMap(String courseName, String noteTitle, String noteContent) {
        String source = noteContent == null ? "" : noteContent.trim();
        if (source.isEmpty()) {
            return "mindmap\n  " + safeMermaidText(noteTitle == null ? "课程笔记" : noteTitle);
        }
        if (!hasKey()) {
            return fallbackMindMap(noteTitle, source);
        }
        if (source.length() > 6000) {
            source = source.substring(0, 6000);
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", "你是教学平台的学习笔记整理助手。请只输出 Mermaid mindmap 语法，不要输出解释文字。"));
        messages.add(message("user", "课程：" + courseName + "\n笔记标题：" + noteTitle
                + "\n请把下面内容整理为 Mermaid mindmap。根节点使用笔记标题，层级控制在 3 层内。\n\n" + source));
        try {
            String reply = sendTextMessages(model, messages);
            if (reply == null || reply.trim().isEmpty()) return fallbackMindMap(noteTitle, source);
            return cleanupMermaid(reply);
        } catch (Exception e) {
            log.error("Mind map generation failed: {}", e.toString());
            return fallbackMindMap(noteTitle, source);
        }
    }

    public String explainImage(String courseName, String resourceTitle, String imageDataUrl) {
        if (!hasKey()) {
            return "AI 助手尚未配置 API Key。请在启动前设置环境变量 AI_API_KEY。";
        }
        if (imageDataUrl == null || !imageDataUrl.startsWith("data:image/")) {
            return "没有收到有效的框选图片。";
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", visionModel);
            body.put("max_tokens", 800);
            ArrayNode messages = body.putArray("messages");
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            ArrayNode content = user.putArray("content");
            ObjectNode text = content.addObject();
            text.put("type", "text");
            text.put("text", "课程：" + courseName + "。视频：" + resourceTitle
                    + "。请解释这张视频框选截图中的知识点，用中文回答，包含概念解释和学习提示。");
            ObjectNode image = content.addObject();
            image.put("type", "image_url");
            ObjectNode imageUrl = image.putObject("image_url");
            imageUrl.put("url", imageDataUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers()),
                    String.class
            );
            return extractReply(response.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("Vision AI call failed: {}", e.getResponseBodyAsString());
            return "AI 图像解释失败：当前模型或接口可能不支持图片输入。请配置支持视觉能力的 AI_API_URL/AI_VISION_MODEL 后再试。";
        } catch (Exception e) {
            log.error("Vision AI call failed: {}", e.toString());
            return "AI 图像解释失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    private String sendTextMessages(String selectedModel, List<Map<String, String>> messages) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", selectedModel);
        body.put("max_tokens", 1000);
        ArrayNode messageNodes = body.putArray("messages");
        for (Map<String, String> message : messages) {
            ObjectNode msg = messageNodes.addObject();
            msg.put("role", message.get("role"));
            msg.put("content", message.get("content"));
        }
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(body), headers()),
                String.class
        );
        return extractReply(response.getBody());
    }

    private String extractReply(String responseBody) throws Exception {
        JsonNode responseJson = mapper.readTree(responseBody);
        JsonNode choices = responseJson.get("choices");
        if (choices != null && choices.size() > 0) {
            return choices.get(0).get("message").get("content").asText();
        }
        String errorMsg = responseJson.has("error") ? responseJson.get("error").toString() : "Unknown response";
        log.error("AI API error: {}", errorMsg);
        return "AI API 返回错误：" + errorMsg;
    }

    private HttpHeaders headers() {
        String key = apiKey == null ? "" : apiKey.trim();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (key.toLowerCase().startsWith("bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, key);
        } else {
            headers.setBearerAuth(key);
        }
        return headers;
    }

    private boolean hasKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String cleanupMermaid(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.startsWith("mindmap") ? cleaned : "mindmap\n" + cleaned;
    }

    private String fallbackMindMap(String noteTitle, String source) {
        String title = safeMermaidText(noteTitle == null || noteTitle.trim().isEmpty() ? "课程笔记" : noteTitle.trim());
        String[] lines = source.split("\\r?\\n");
        StringBuilder map = new StringBuilder("mindmap\n  ").append(title).append('\n');
        int count = 0;
        for (String line : lines) {
            String item = line.replaceAll("^[#\\-\\*\\d\\.\\s]+", "").trim();
            if (item.isEmpty()) continue;
            map.append("    ").append(safeMermaidText(item.length() > 32 ? item.substring(0, 32) : item)).append('\n');
            if (++count >= 8) break;
        }
        if (count == 0) {
            map.append("    核心知识点\n    复习提示\n");
        }
        return map.toString();
    }

    private String safeMermaidText(String text) {
        return text.replaceAll("[\\r\\n\"`]", " ").trim();
    }
}

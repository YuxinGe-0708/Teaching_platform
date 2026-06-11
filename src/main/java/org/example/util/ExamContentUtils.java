package org.example.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExamContentUtils {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FEEDBACK_START = "\n\n<!--TP_GRADE";
    private static final String FEEDBACK_END = "TP_GRADE-->";

    private ExamContentUtils() {
    }

    public static Map<String, Object> normalizeContent(String raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answers", new LinkedHashMap<String, Object>());
        result.put("attachments", new LinkedHashMap<String, Object>());
        if (raw == null || raw.trim().isEmpty()) return result;

        try {
            Map<String, Object> parsed = JSON.readValue(raw, new TypeReference<Map<String, Object>>() {});
            Object answers = parsed.get("answers");
            Object attachments = parsed.get("attachments");
            if (answers instanceof Map) {
                result.put("answers", copyMap((Map<?, ?>) answers));
                result.put("attachments", attachments instanceof Map ? copyMap((Map<?, ?>) attachments) : new LinkedHashMap<String, Object>());
            } else {
                result.put("answers", copyMap(parsed));
            }
        } catch (Exception ignored) {
            Map<String, Object> answers = new LinkedHashMap<>();
            answers.put("1", raw);
            result.put("answers", answers);
        }
        return result;
    }

    public static String toJson(Map<String, Object> content) {
        try {
            return JSON.writeValueAsString(content == null ? normalizeContent("") : content);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String firstAnswerText(String raw) {
        Map<String, Object> content = normalizeContent(raw);
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) content.get("answers");
        Object answer = answers.get("1");
        if (answer == null && !answers.isEmpty()) {
            answer = answers.values().iterator().next();
        }
        return answer == null ? "" : String.valueOf(answer);
    }

    public static int attachmentCount(String raw) {
        Map<String, Object> content = normalizeContent(raw);
        @SuppressWarnings("unchecked")
        Map<String, Object> attachments = (Map<String, Object>) content.get("attachments");
        int total = 0;
        for (Object value : attachments.values()) {
            total += attachmentList(value).size();
        }
        return total;
    }

    public static String addAttachment(String raw, String questionId, String filePath, String originalName) {
        Map<String, Object> content = normalizeContent(raw);
        @SuppressWarnings("unchecked")
        Map<String, Object> attachments = (Map<String, Object>) content.get("attachments");
        List<Map<String, String>> files = attachmentList(attachments.get(questionId));
        Map<String, String> file = new LinkedHashMap<>();
        file.put("path", filePath);
        file.put("name", originalName == null || originalName.trim().isEmpty()
                ? filenameFromPath(filePath)
                : Paths.get(originalName).getFileName().toString());
        files.add(file);
        attachments.put(questionId, files);
        return toJson(content);
    }

    public static List<Map<String, Object>> questionRows(String questionsJson, String contentJson, String feedback) {
        List<Map<String, Object>> questions = parseQuestions(questionsJson);
        Map<String, Object> content = normalizeContent(contentJson);
        Map<String, String> grades = metadataMap(feedback, "scores");
        Map<String, String> comments = metadataMap(feedback, "comments");
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) content.get("answers");
        @SuppressWarnings("unchecked")
        Map<String, Object> attachments = (Map<String, Object>) content.get("attachments");

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> question : questions) {
            String id = String.valueOf(question.get("id"));
            Map<String, Object> row = new LinkedHashMap<>(question);
            Object answer = answers.get(id);
            row.put("answer", answer == null ? "" : String.valueOf(answer));
            row.put("attachments", attachmentList(attachments.get(id)));
            row.put("grade", grades.get(id));
            row.put("comment", comments.get(id));
            rows.add(row);
        }
        return rows;
    }

    public static String buildFeedback(String overallComment, String scoresJson, String commentsJson) {
        Map<String, String> scores = parseStringMap(scoresJson);
        Map<String, String> comments = parseStringMap(commentsJson);
        if (scores.isEmpty() && comments.isEmpty()) {
            return overallComment == null ? "" : overallComment.trim();
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("scores", scores);
        meta.put("comments", comments);
        String visible = overallComment == null ? "" : overallComment.trim();
        return visible + FEEDBACK_START + "\n" + toJson(meta) + "\n" + FEEDBACK_END;
    }

    public static String visibleFeedback(String feedback) {
        if (feedback == null) return null;
        int start = feedback.indexOf(FEEDBACK_START);
        return start < 0 ? feedback : feedback.substring(0, start).trim();
    }

    private static List<Map<String, Object>> parseQuestions(String questionsJson) {
        try {
            return JSON.readValue(questionsJson == null ? "[]" : questionsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static Map<String, String> metadataMap(String feedback, String key) {
        if (feedback == null) return Collections.emptyMap();
        int start = feedback.indexOf(FEEDBACK_START);
        int end = feedback.indexOf(FEEDBACK_END);
        if (start < 0 || end <= start) return Collections.emptyMap();
        try {
            String json = feedback.substring(start + FEEDBACK_START.length(), end).trim();
            Map<String, Object> meta = JSON.readValue(json, new TypeReference<Map<String, Object>>() {});
            return parseStringMap(JSON.writeValueAsString(meta.get(key)));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static Map<String, String> parseStringMap(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null || raw.trim().isEmpty()) return result;
        try {
            Map<String, Object> parsed = JSON.readValue(raw, new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                if (entry.getValue() != null) result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }

    private static List<Map<String, String>> attachmentList(Object value) {
        List<Map<String, String>> files = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item instanceof Map) {
                    Map<?, ?> raw = (Map<?, ?>) item;
                    String path = raw.get("path") == null ? "" : String.valueOf(raw.get("path"));
                    if (path.trim().isEmpty()) continue;
                    Map<String, String> file = new LinkedHashMap<>();
                    file.put("path", path);
                    file.put("name", raw.get("name") == null ? filenameFromPath(path) : String.valueOf(raw.get("name")));
                    files.add(file);
                }
            }
        }
        return files;
    }

    private static String filenameFromPath(String filePath) {
        try {
            Path filename = Paths.get(filePath).getFileName();
            return DownloadUtils.displayFilename(filename == null ? "attachment" : filename.toString());
        } catch (Exception e) {
            return "attachment";
        }
    }
}

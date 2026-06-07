package org.example.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TaskMetadataUtils {
    private static final String START = "<!--TP_META";
    private static final String END = "TP_META-->";

    private TaskMetadataUtils() {
    }

    public static String buildDescription(String markdown, String examAnswer, String sampleInput, String expectedOutput) {
        return buildDescription(markdown, examAnswer, sampleInput, expectedOutput, null);
    }

    public static String buildDescription(String markdown, String examAnswer, String sampleInput, String expectedOutput, String testCases) {
        String body = markdown == null ? "" : markdown;
        StringBuilder meta = new StringBuilder();
        append(meta, "examAnswer", examAnswer);
        append(meta, "sampleInput", sampleInput);
        append(meta, "expectedOutput", expectedOutput);
        append(meta, "testCases", testCases);
        if (meta.length() == 0) return body;
        return body + "\n\n" + START + "\n" + meta + END;
    }

    public static String visibleMarkdown(String description) {
        if (description == null) return "";
        int start = description.indexOf(START);
        if (start < 0) return description;
        return description.substring(0, start).trim();
    }

    public static String examAnswer(String description) {
        return value(description, "examAnswer");
    }

    public static String sampleInput(String description) {
        return value(description, "sampleInput");
    }

    public static String expectedOutput(String description) {
        return value(description, "expectedOutput");
    }

    public static String testCases(String description) {
        return value(description, "testCases");
    }

    public static String testCasesJson(String description) {
        String configured = testCases(description);
        if (configured != null && !configured.trim().isEmpty()) {
            return parseConfiguredCases(configured);
        }
        String expected = expectedOutput(description);
        if (expected == null || expected.trim().isEmpty()) {
            expected = "Hello World";
        }
        String input = sampleInput(description);
        return "[{\"input\":\"" + json(input == null ? "" : input) + "\",\"expectedOutput\":\"" + json(expected) + "\"}]";
    }

    private static String parseConfiguredCases(String raw) {
        String[] lines = raw.split("\\R");
        StringBuilder jsonArray = new StringBuilder("[");
        int count = 0;
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            String[] parts = line.split("\\|", -1);
            String input = parts.length > 0 ? parts[0].trim() : "";
            String expected = parts.length > 1 ? parts[1].trim() : "";
            String weight = parts.length > 2 ? parts[2].trim() : "1";
            if (count++ > 0) jsonArray.append(",");
            jsonArray.append("{\"input\":\"").append(json(input))
                    .append("\",\"expectedOutput\":\"").append(json(expected))
                    .append("\",\"weight\":\"").append(json(weight)).append("\"}");
        }
        jsonArray.append("]");
        return count == 0 ? "[]" : jsonArray.toString();
    }

    private static void append(StringBuilder meta, String key, String value) {
        if (value == null || value.trim().isEmpty()) return;
        meta.append(key)
                .append("=")
                .append(Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)))
                .append("\n");
    }

    private static String value(String description, String key) {
        if (description == null) return "";
        int start = description.indexOf(START);
        int end = description.indexOf(END);
        if (start < 0 || end <= start) return "";
        String[] lines = description.substring(start + START.length(), end).split("\\R");
        for (String line : lines) {
            String prefix = key + "=";
            if (line.startsWith(prefix)) {
                try {
                    return new String(Base64.getDecoder().decode(line.substring(prefix.length())), StandardCharsets.UTF_8);
                } catch (IllegalArgumentException ignored) {
                    return "";
                }
            }
        }
        return "";
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}

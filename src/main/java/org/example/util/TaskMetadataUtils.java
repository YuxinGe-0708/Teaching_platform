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
        return parseMultiLineCases(raw.trim());
    }

    private static String parseMultiLineCases(String raw) {
        String[] lines = raw.split("\\R");
        java.util.List<TestCase> cases = new java.util.ArrayList<>();

        StringBuilder currentInput = new StringBuilder();
        StringBuilder currentOutput = new StringBuilder();
        StringBuilder currentWeight = new StringBuilder();
        String section = null;

        for (String line : lines) {
            String t = line.trim();
            if (t.matches("-{3,}CASE-{3,}")) {
                if (section != null) {
                    addMultiLineCase(cases, currentInput, currentOutput, currentWeight);
                    currentInput = new StringBuilder();
                    currentOutput = new StringBuilder();
                    currentWeight = new StringBuilder();
                }
                section = "input";
            } else if (t.matches("-{3,}OUTPUT-{3,}")) {
                section = "output";
            } else if (t.matches("-{3,}WEIGHT-{3,}")) {
                section = "weight";
            } else if (section != null) {
                StringBuilder buf;
                switch (section) {
                    case "output": buf = currentOutput; break;
                    case "weight": buf = currentWeight; break;
                    default: buf = currentInput;
                }
                if (buf.length() > 0) buf.append("\n");
                buf.append(line);
            }
        }
        if (section != null) {
            addMultiLineCase(cases, currentInput, currentOutput, currentWeight);
        }

        return buildMultiLineJson(cases);
    }

    private static void addMultiLineCase(java.util.List<TestCase> cases,
                                          StringBuilder input, StringBuilder output, StringBuilder weight) {
        String in = input.toString().trim();
        String out = output.toString().trim();
        if (in.isEmpty() && out.isEmpty()) return;
        String w = weight.toString().trim();
        if (w.isEmpty()) w = "1";
        cases.add(new TestCase(in, out, w));
    }

    private static String buildMultiLineJson(java.util.List<TestCase> cases) {
        if (cases.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cases.size(); i++) {
            if (i > 0) sb.append(",");
            TestCase tc = cases.get(i);
            sb.append("{\"input\":\"").append(json(tc.input))
              .append("\",\"expectedOutput\":\"").append(json(tc.expectedOutput))
              .append("\",\"weight\":\"").append(json(tc.weight)).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static class TestCase {
        final String input, expectedOutput, weight;
        TestCase(String in, String out, String w) {
            input = in; expectedOutput = out; weight = w;
        }
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

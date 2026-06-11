package org.example.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class TaskMetadataUtils {
    private static final String START = "<!--TP_META";
    private static final String END = "TP_META-->";

    private TaskMetadataUtils() {
    }

    public static String buildDescription(String markdown, String examAnswer, String testCases, String allowedLanguage, String examQuestions) {
        String body = markdown == null ? "" : markdown;
        StringBuilder meta = new StringBuilder();
        append(meta, "examAnswer", examAnswer);
        append(meta, "testCases", testCases);
        append(meta, "allowedLanguage", normalizeLanguage(allowedLanguage));
        append(meta, "examQuestions", examQuestions);
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

    public static String allowedLanguage(String description) {
        String value = normalizeLanguage(value(description, "allowedLanguage"));
        return value == null || value.isEmpty() ? "python" : value;
    }

    public static String examQuestions(String description) {
        return value(description, "examQuestions");
    }

    public static String testCasesJson(String description) {
        String configured = testCases(description);
        if (configured != null && !configured.trim().isEmpty()) {
            String parsed = parseMultiLineCases(configured.trim());
            if (!"[]".equals(parsed)) return parsed;
        }
        String expected = expectedOutput(description);
        if (expected != null && !expected.trim().isEmpty()) {
            String input = sampleInput(description);
            return "[{\"input\":\"" + json(input == null ? "" : input) + "\",\"expectedOutput\":\"" + json(expected) + "\"}]";
        }
        return "[]";
    }

    public static String examQuestionsJson(String description) {
        List<ExamQuestion> questions = parseExamQuestions(examQuestions(description));
        if (questions.isEmpty()) {
            String visible = visibleMarkdown(description);
            if (visible == null || visible.trim().isEmpty()) visible = "请完成本题作答。";
            questions.add(new ExamQuestion("1", "简答题", "short", visible.trim(), 100));
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < questions.size(); i++) {
            if (i > 0) sb.append(",");
            ExamQuestion q = questions.get(i);
            sb.append("{\"id\":\"").append(json(q.id)).append("\"")
                    .append(",\"title\":\"").append(json(q.title)).append("\"")
                    .append(",\"type\":\"").append(json(q.type)).append("\"")
                    .append(",\"content\":\"").append(json(q.content)).append("\"")
                    .append(",\"score\":").append(q.score)
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String parseMultiLineCases(String raw) {
        String[] lines = raw.split("\\R");
        List<TestCase> cases = new ArrayList<>();
        StringBuilder currentInput = new StringBuilder();
        StringBuilder currentOutput = new StringBuilder();
        StringBuilder currentWeight = new StringBuilder();
        String section = null;

        for (String line : lines) {
            String t = line.trim();
            if (isMarker(t, "CASE")) {
                if (section != null) {
                    addMultiLineCase(cases, currentInput, currentOutput, currentWeight);
                    currentInput = new StringBuilder();
                    currentOutput = new StringBuilder();
                    currentWeight = new StringBuilder();
                }
                section = "input";
            } else if (isMarker(t, "OUTPUT")) {
                section = "output";
            } else if (isMarker(t, "WEIGHT")) {
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

    private static boolean isMarker(String line, String marker) {
        if (line == null || marker == null) return false;
        return line.matches("(?i)-{3,}\\s*" + marker + "\\s*-{3,}");
    }

    private static List<ExamQuestion> parseExamQuestions(String raw) {
        List<ExamQuestion> questions = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return questions;

        String[] blocks = raw.split("(?m)^\\s*-{3,}QUESTION-{3,}\\s*$");
        int index = 1;
        for (String block : blocks) {
            if (block == null || block.trim().isEmpty()) continue;
            String title = "第 " + index + " 题";
            String type = "short";
            int score = 100;
            StringBuilder content = new StringBuilder();
            for (String line : block.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("title:") || trimmed.startsWith("标题:")) {
                    title = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                } else if (trimmed.startsWith("type:") || trimmed.startsWith("类型:")) {
                    type = normalizeQuestionType(trimmed.substring(trimmed.indexOf(':') + 1).trim());
                } else if (trimmed.startsWith("score:") || trimmed.startsWith("分值:")) {
                    score = parseScore(trimmed.substring(trimmed.indexOf(':') + 1).trim(), score);
                } else if (trimmed.startsWith("answer:") || trimmed.startsWith("答案:")) {
                    // Answers stay server-side in metadata and are not exposed to students.
                } else {
                    if (content.length() > 0) content.append("\n");
                    content.append(line);
                }
            }
            String text = content.toString().trim();
            if (!text.isEmpty()) {
                questions.add(new ExamQuestion(String.valueOf(index), title, type, text, score));
                index++;
            }
        }
        return questions;
    }

    private static void addMultiLineCase(List<TestCase> cases, StringBuilder input, StringBuilder output, StringBuilder weight) {
        String in = input.toString().trim();
        String out = output.toString().trim();
        if (in.isEmpty() && out.isEmpty()) return;
        String w = weight.toString().trim();
        if (w.isEmpty()) w = "1";
        cases.add(new TestCase(in, out, w));
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

    private static String normalizeLanguage(String language) {
        if (language == null) return "";
        String value = language.trim().toLowerCase();
        if ("py".equals(value)) return "python";
        if ("gcc".equals(value)) return "c";
        if ("any".equals(value)) return "any";
        if ("python".equals(value) || "java".equals(value) || "c".equals(value)) return value;
        return "";
    }

    private static String normalizeQuestionType(String type) {
        String value = type == null ? "" : type.trim().toLowerCase();
        if (value.contains("choice") || value.contains("选择")) return "choice";
        if (value.contains("upload") || value.contains("附件")) return "upload";
        return "short";
    }

    private static int parseScore(String raw, int fallback) {
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String json(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
            }
        }
        return escaped.toString();
    }

    private static class TestCase {
        final String input;
        final String expectedOutput;
        final String weight;

        TestCase(String input, String expectedOutput, String weight) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.weight = weight;
        }
    }

    private static class ExamQuestion {
        final String id;
        final String title;
        final String type;
        final String content;
        final int score;

        ExamQuestion(String id, String title, String type, String content, int score) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.content = content;
            this.score = score;
        }
    }
}

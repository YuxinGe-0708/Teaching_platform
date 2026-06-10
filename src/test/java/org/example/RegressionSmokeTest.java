package org.example;

import org.example.util.TaskMetadataUtils;

public class RegressionSmokeTest {
    public static void main(String[] args) {
        String description = TaskMetadataUtils.buildDescription(
                "考试说明",
                "",
                "1 2",
                "3",
                "---CASE---\n1 2\n---OUTPUT---\n3\n---WEIGHT---\n2",
                "java",
                "---QUESTION---\ntitle: 第一题\ntype: short\nscore: 40\n请简述事务特性。\n"
                        + "---QUESTION---\ntitle: 第二题\ntype: upload\nscore: 60\n请上传设计附件。"
        );

        assertContains(TaskMetadataUtils.testCasesJson(description), "\"weight\":\"2\"", "test case weight");
        assertEquals("java", TaskMetadataUtils.allowedLanguage(description), "allowed language");
        assertContains(TaskMetadataUtils.examQuestionsJson(description), "第一题", "exam question title");
        assertContains(TaskMetadataUtils.examQuestionsJson(description), "\"type\":\"upload\"", "upload question type");

        String fallback = TaskMetadataUtils.buildDescription("单题内容", "", "", "", null);
        assertContains(TaskMetadataUtils.examQuestionsJson(fallback), "单题内容", "fallback exam question");
        assertEquals("python", TaskMetadataUtils.allowedLanguage(fallback), "default allowed language");

        System.out.println("RegressionSmokeTest passed");
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(label + " expected " + expected + " but got " + actual);
        }
    }

    private static void assertContains(String text, String needle, String label) {
        if (text == null || !text.contains(needle)) {
            throw new IllegalStateException(label + " missing " + needle + " in " + text);
        }
    }
}

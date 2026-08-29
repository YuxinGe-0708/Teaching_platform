package org.example.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TaskMetadataUtilsUnitTest {

  // ========== T010: 任务元数据Base64编码与解析（正例） ==========
  @Test
  void buildDescription_shouldEncodeMetadataCorrectly() {
    // Given
    String markdown = "请实现快排";
    String examAnswer = "A";
    String testCases = "1 2\n3";
    String allowedLanguage = "python";

    // When
    String result = TaskMetadataUtils.buildDescription(markdown, examAnswer, testCases, allowedLanguage, null);

    // Then
    assertTrue(result.contains("<!--TP_META"));
    assertTrue(result.contains("TP_META-->"));
    assertTrue(result.contains("examAnswer="));
    assertTrue(result.contains("testCases="));
    assertTrue(result.contains("allowedLanguage="));
  }

  @Test
  void visibleMarkdown_shouldReturnOnlyVisiblePart() {
    // Given
    String description = "可见内容\n\n<!--TP_META\nexamAnswer=QQ==\nTP_META-->";

    // When
    String result = TaskMetadataUtils.visibleMarkdown(description);

    // Then
    assertEquals("可见内容", result);
  }

  @Test
  void visibleMarkdown_shouldReturnWholeString_whenNoMeta() {
    // Given
    String description = "纯可见内容";

    // When
    String result = TaskMetadataUtils.visibleMarkdown(description);

    // Then
    assertEquals("纯可见内容", result);
  }

  @Test
  void examAnswer_shouldExtractCorrectly() {
    // Given
    String description = "请实现快排\n\n<!--TP_META\nexamAnswer=QQ==\nTP_META-->";

    // When
    String result = TaskMetadataUtils.examAnswer(description);

    // Then
    assertEquals("A", result);
  }

  @Test
  void allowedLanguage_shouldReturnPython_whenNotSpecified() {
    // Given
    String description = "请实现快排\n\n<!--TP_META\nTP_META-->";

    // When
    String result = TaskMetadataUtils.allowedLanguage(description);

    // Then
    assertEquals("python", result);
  }

  @Test
  void allowedLanguage_shouldReturnSpecifiedValue() {
    // Given
    String description = "请实现快排\n\n<!--TP_META\nallowedLanguage=amF2YQ==\nTP_META-->";

    // When
    String result = TaskMetadataUtils.allowedLanguage(description);

    // Then
    assertEquals("java", result);
  }

  @Test
  void testCasesJson_shouldReturnEmptyArray_whenNoTestCases() {
    // Given
    String description = "请实现快排";

    // When
    String result = TaskMetadataUtils.testCasesJson(description);

    // Then
    assertEquals("[]", result);
  }

  @Test
  void testCasesJson_shouldParseMultiLineFormat() {
    // Given
    String description = "编程题\n\n<!--TP_META\ntestCases=LS0tQ0FTRS0tLQppbnB1dDEKLS0tT1VUUFVVVC0tLQpvdXRwdXQxCi0tLUNBU0UtLS0KaW5wdXQyCi0tLU9VVFBVVC0tLQpvdXRwdXQyCg==\nTP_META-->";

    // When
    String result = TaskMetadataUtils.testCasesJson(description);

    // Then
    assertTrue(result.startsWith("["));
    assertTrue(result.endsWith("]"));
  }
}

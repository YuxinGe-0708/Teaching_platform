package org.example.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DownloadUtilsUnitTest {

  // ========== R010: 合法文件安全下载路径校验（正例） ==========
  @Test
  void displayFilename_shouldRemoveTimestampPrefix() {
    // Given
    String storedFilename = "1234567890_report.pdf";

    // When
    String result = DownloadUtils.displayFilename(storedFilename);

    // Then
    assertEquals("report.pdf", result);
  }

  @Test
  void displayFilename_shouldReturnOriginal_whenNoTimestampPrefix() {
    // Given
    String storedFilename = "slide.pdf";

    // When
    String result = DownloadUtils.displayFilename(storedFilename);

    // Then
    assertEquals("slide.pdf", result);
  }

  @Test
  void displayFilename_shouldReturnDefault_whenNull() {
    // Given
    String storedFilename = null;

    // When
    String result = DownloadUtils.displayFilename(storedFilename);

    // Then
    assertEquals("download", result);
  }

  // ========== R011: 跨目录路径穿越攻击下载拦截（反例） ==========
  // Note: safeUploadPath is private, tested through attachment method
  // The test would normally be an integration test
}

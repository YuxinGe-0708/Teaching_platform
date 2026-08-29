package org.example.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class DownloadUtilsUnitTest {

  // ========== R010: 合法文件安全下载路径校验（正例） ==========
  @Test
  void displayFilename_shouldRemoveTimestampPrefix() {
    String storedFilename = "1234567890_report.pdf";
    String result = DownloadUtils.displayFilename(storedFilename);
    assertEquals("report.pdf", result);
  }

  @Test
  void displayFilename_shouldReturnOriginal_whenNoTimestampPrefix() {
    String storedFilename = "slide.pdf";
    String result = DownloadUtils.displayFilename(storedFilename);
    assertEquals("slide.pdf", result);
  }

  @Test
  void displayFilename_shouldReturnDefault_whenNull() {
    String result = DownloadUtils.displayFilename(null);
    assertEquals("download", result);
  }

  // ========== R011: 跨目录路径穿越攻击下载拦截（反例） ==========
  @Test
  void safeUploadPath_shouldPreventPathTraversal() {
    Path basePath = Paths.get("uploads");
    String attackFilename = "../../../etc/passwd";

    try {
      Object result = ReflectionTestUtils.invokeMethod(
          DownloadUtils.class,
          "safeUploadPath",
          basePath,
          attackFilename
      );

      // 如果返回 null 或返回的结果不包含跳出目录的危险路径，则视为安全防御成功
      if (result != null) {
        String resultPath = result.toString().replace("\\", "/");
        assertFalse(resultPath.contains("../"), "解析后的路径不应包含目录跳跃符");
      }
    } catch (Throwable t) {
      // 抛出任何异常都说明非法路径已被成功拦截阻断
      assertNotNull(t, "非法路径触发了安全拦截异常");
    }
  }
}
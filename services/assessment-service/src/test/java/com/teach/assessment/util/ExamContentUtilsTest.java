package com.teach.assessment.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExamContentUtilsTest {

  @Test
  @DisplayName("UNIT-TC-UTIL-01: 标准化解析作答 JSON（包含容错纯文本输入）")
  void testNormalizeContent() {
    // 纯文本容错为题号 1 的答案
    Map<String, Object> textRes = ExamContentUtils.normalizeContent("答案是A");
    assertEquals("答案是A", ExamContentUtils.firstAnswerText("答案是A"));

    // 标准 JSON 结构
    String json = "{\"answers\":{\"1\":\"Option A\",\"2\":\"Option B\"}}";
    Map<String, Object> jsonRes = ExamContentUtils.normalizeContent(json);
    assertNotNull(jsonRes.get("answers"));
    assertEquals("Option A", ExamContentUtils.firstAnswerText(json));
  }

  @Test
  @DisplayName("UNIT-TC-UTIL-02: 附件添加与数量统计")
  void testAttachmentOperations() {
    String baseJson = "{\"answers\":{},\"attachments\":{}}";
    String updated = ExamContentUtils.addAttachment(baseJson, "1", "/uploads/report.pdf", "实验报告.pdf");

    assertEquals(1, ExamContentUtils.attachmentCount(updated));
  }

  @Test
  @DisplayName("UNIT-TC-UTIL-03: 评语元数据构建与学生可见文本提取")
  void testFeedbackBuildingAndExtracting() {
    String feedback = ExamContentUtils.buildFeedback("批改完成，表现很好！", "{\"1\":\"100\"}", "{\"1\":\"完全正确\"}");

    // 包含隐藏元数据标记
    assertTrue(feedback.contains("<!--TP_GRADE"));
    // 提取对外展示的评语文本
    assertEquals("批改完成，表现很好！", ExamContentUtils.visibleFeedback(feedback));
  }
}

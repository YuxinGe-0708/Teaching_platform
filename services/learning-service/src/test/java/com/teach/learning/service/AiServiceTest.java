package com.teach.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceTest {

  private AiService aiService;

  @BeforeEach
  void setUp() {
    aiService = new AiService();
    // 单元测试中未配置 API Key 时，验证各接口的优雅降级与防御分支
  }

  @Test
  @DisplayName("UNIT-TC-AI-01: 未配置 API Key 时对话与总结返回提示信息")
  void testUnconfiguredGracefulReturn() {
    String chatResult = aiService.chat("session-1", "软件工程", "你好");
    String summaryResult = aiService.summarize("软件工程", "第一章", "长文本资料");
    String imageResult = aiService.explainImage("软件工程", "架构图", "base64/url");

    assertEquals("AI 助手尚未配置 API Key。", chatResult);
    assertEquals("AI 助手尚未配置 API Key。", summaryResult);
    assertEquals("AI 助手尚未配置 API Key。", imageResult);
  }

  @Test
  @DisplayName("UNIT-TC-AI-02: 未配置 API Key 时脑图生成默认 Mermaid 结构")
  void testMindMapDefault() {
    String mindMap = aiService.mindMap("软件工程", "微服务架构", "文本");

    assertTrue(mindMap.contains("mindmap"));
    assertTrue(mindMap.contains("微服务架构"));
  }

  @Test
  @DisplayName("UNIT-TC-AI-03: 清除对话 Session 正常执行")
  void testClearSession() {
    assertDoesNotThrow(() -> aiService.clear("session-1"));
  }
}
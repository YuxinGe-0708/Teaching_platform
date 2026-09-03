package com.teach.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeServiceTest {

  @Mock
  private LocalJudgeService localJudgeService;

  @InjectMocks
  private JudgeService judgeService;

  @Test
  @DisplayName("UNIT-TC-JUDGE-01: 未提供测试用例时返回系统内部错误（IE）")
  void testJudge_NoTestCases() {
    JudgeService.JudgeResult result = judgeService.judge("print(1)", "python", Collections.emptyList());

    assertNotNull(result);
    assertEquals("IE", result.status);
    assertTrue(result.errorMessage.contains("没有配置测试用例"));
  }

  @Test
  @DisplayName("UNIT-TC-JUDGE-02: 传入不支持的编程语言返回 IE")
  void testJudge_UnsupportedLanguage() {
    Map<String, String> tc = new HashMap<>();
    tc.put("input", "1");
    tc.put("expectedOutput", "1");

    JudgeService.JudgeResult result = judgeService.judge("fn main() {}", "rust", Collections.singletonList(tc));

    assertEquals("IE", result.status);
    assertTrue(result.errorMessage.contains("暂不支持该语言"));
  }

  @Test
  @DisplayName("UNIT-TC-JUDGE-03: 云端连接失败时自动触发本地沙箱评测（本地降级）")
  void testJudge_FallbackToLocalJudge() {
    // 设置启用本地回退
    ReflectionTestUtils.setField(judgeService, "localFallbackEnabled", true);
    ReflectionTestUtils.setField(judgeService, "apiUrl", "http://127.0.0.1:9999/unreachable"); // 触发 RestClientException

    Map<String, String> tc = new HashMap<>();
    tc.put("input", "1 2");
    tc.put("expectedOutput", "3");
    tc.put("weight", "1");

    JudgeService.JudgeResult localMockResult = new JudgeService.JudgeResult();
    localMockResult.status = "AC";
    localMockResult.score = 100.0;
    localMockResult.passedCases = 1;

    when(localJudgeService.judge(any(), any(), any())).thenReturn(localMockResult);

    JudgeService.JudgeResult result = judgeService.judge("a, b = map(int, input().split())\nprint(a+b)", "python", Collections.singletonList(tc));

    assertNotNull(result);
    assertTrue(result.usedLocalJudge);
    assertEquals("AC", result.status);
    verify(localJudgeService).judge(any(), any(), any());
  }

  @Test
  @DisplayName("UNIT-TC-JUDGE-04: 测试用例权重解析测试")
  void testParseWeight() {
    assertEquals(3, JudgeService.parseWeight("3"));
    assertEquals(1, JudgeService.parseWeight(null));
    assertEquals(1, JudgeService.parseWeight("invalid"));
    assertEquals(1, JudgeService.parseWeight("-5"));
  }
}
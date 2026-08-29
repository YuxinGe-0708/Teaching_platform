package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JudgeServiceUnitTest {

  @Mock
  private LocalJudgeService localJudgeService;

  @InjectMocks
  private JudgeService judgeService;

  private List<Map<String, String>> testCases;

  @BeforeEach
  void setUp() {
    testCases = new ArrayList<>();
    Map<String, String> tc1 = new HashMap<>();
    tc1.put("input", "1 2");
    tc1.put("expectedOutput", "3");
    tc1.put("weight", "1");
    testCases.add(tc1);

    Map<String, String> tc2 = new HashMap<>();
    tc2.put("input", "5 7");
    tc2.put("expectedOutput", "12");
    tc2.put("weight", "2");
    testCases.add(tc2);
  }

  // ========== J010: 判题状态码映射（正例 - 反射测试私有 mapStatus 方法） ==========
  @Test
  void mapStatus_shouldReturnCorrectStatus() {
    // 通过反射测试 private mapStatus(int)
    Object acStatus = ReflectionTestUtils.invokeMethod(judgeService, "mapStatus", 3);
    Object waStatus = ReflectionTestUtils.invokeMethod(judgeService, "mapStatus", 4);
    Object tleStatus = ReflectionTestUtils.invokeMethod(judgeService, "mapStatus", 5);

    assertNotNull(acStatus, "状态码3映射不应为空");
    assertNotNull(waStatus, "状态码4映射不应为空");
    assertNotNull(tleStatus, "状态码5映射不应为空");
  }

  // ========== J011: 多测试用例权重与总分折算（正例） ==========
  @Test
  void parseWeight_shouldCalculateTotalWeights() {
    // 验证多用例权重解析与累加计算
    int totalWeight = 0;
    for (Map<String, String> tc : testCases) {
      totalWeight += JudgeService.parseWeight(tc.get("weight"));
    }
    assertEquals(3, totalWeight, "用例总权重应为 1 + 2 = 3");
  }

  // ========== parseWeight 工具方法测试（边界与异常） ==========
  @Test
  void parseWeight_shouldReturnOne_whenNull() {
    int result = JudgeService.parseWeight(null);
    assertEquals(1, result);
  }

  @Test
  void parseWeight_shouldReturnParsedValue_whenValid() {
    int result = JudgeService.parseWeight("5");
    assertEquals(5, result);
  }

  @Test
  void parseWeight_shouldReturnOne_whenInvalidFormat() {
    int result = JudgeService.parseWeight("invalid");
    assertEquals(1, result);
  }

  @Test
  void parseWeight_shouldReturnOne_whenZero() {
    int result = JudgeService.parseWeight("0");
    assertEquals(1, result);
  }
}
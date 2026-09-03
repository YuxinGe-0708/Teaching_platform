package com.teach.user.service;

import com.teach.user.entity.OperationLog;
import com.teach.user.mapper.OperationLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

  @Mock
  private OperationLogMapper operationLogMapper;

  @InjectMocks
  private OperationLogService operationLogService;

  @Test
  @DisplayName("UNIT-TC-LOG-01: 记录操作日志成功")
  void testRecordLog() {
    when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

    operationLogService.record(1L, "student1", "UPDATE_PROFILE", "更新了邮箱");

    ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
    verify(operationLogMapper).insert(captor.capture());

    OperationLog captured = captor.getValue();
    assertEquals(1L, captured.getUserId());
    assertEquals("student1", captured.getUsername());
    assertEquals("UPDATE_PROFILE", captured.getAction());
    assertEquals("更新了邮箱", captured.getDetail());
  }

  @Test
  @DisplayName("UNIT-TC-LOG-02: 查询用户日志与全站近期日志")
  void testFindLogs() {
    OperationLog log = new OperationLog();
    log.setId(100L);
    when(operationLogMapper.findByUserId(1L)).thenReturn(Collections.singletonList(log));
    when(operationLogMapper.findRecent()).thenReturn(Collections.singletonList(log));

    List<OperationLog> userLogs = operationLogService.findByUserId(1L);
    List<OperationLog> recentLogs = operationLogService.findRecent();

    assertNotNull(userLogs);
    assertEquals(1, userLogs.size());
    assertEquals(1, recentLogs.size());
  }
}
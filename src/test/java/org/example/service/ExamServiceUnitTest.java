package org.example.service;

import org.example.entity.ExamRecord;
import org.example.entity.Task;
import org.example.mapper.ExamRecordMapper;
import org.example.mapper.SubmissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExamServiceUnitTest {

  @Mock
  private ExamRecordMapper examRecordMapper;

  @Mock
  private SubmissionMapper submissionMapper;

  @InjectMocks
  private ExamService examService;

  private ExamRecord testRecord;
  private Task testTask;

  @BeforeEach
  void setUp() {
    testRecord = new ExamRecord();
    testRecord.setId(1L);
    testRecord.setTaskId(60L);
    testRecord.setStudentId(5L);
    testRecord.setStatus("IN_PROGRESS");
    testRecord.setStartTime(new Timestamp(System.currentTimeMillis()));
    testRecord.setContent("{\"answers\":{\"1\":\"A\",\"2\":\"简答进行中...\"}}");

    testTask = new Task();
    testTask.setId(60L);
    testTask.setType("exam");
    testTask.setMaxScore(100);
    testTask.setEndTime(new Timestamp(System.currentTimeMillis() + 3600000));
  }

  // ========== T050: 考试剩余时间计算逻辑（正例/边界） ==========
  @Test
  void getRemainingSeconds_shouldReturnCorrectRemainingTime() {
    // Given
    Timestamp now = new Timestamp(System.currentTimeMillis());
    testTask.setEndTime(new Timestamp(now.getTime() + 3600000));

    // When
    long remaining = examService.getRemainingSeconds(testRecord, testTask);

    // Then
    assertTrue(remaining > 3500 && remaining <= 3600);
  }

  @Test
  void getRemainingSeconds_shouldReturnZero_whenTaskEndTimeNull() {
    // Given
    testTask.setEndTime(null);

    // When
    long remaining = examService.getRemainingSeconds(testRecord, testTask);

    // Then
    assertEquals(7200, remaining);
  }

  @Test
  void isExamTimeUp_shouldReturnFalse_whenTimeRemaining() {
    // Given
    Timestamp now = new Timestamp(System.currentTimeMillis());
    testTask.setEndTime(new Timestamp(now.getTime() + 3600000));

    // When
    boolean result = examService.isExamTimeUp(testRecord, testTask);

    // Then
    assertFalse(result);
  }

  // ========== 开始考试 ==========
  @Test
  void beginExam_shouldCreateNewRecord_whenNoExistingRecord() {
    // Given
    Long studentId = 5L;
    Long taskId = 60L;

    when(examRecordMapper.findByStudentAndTask(studentId, taskId)).thenReturn(null);
    when(examRecordMapper.insert(any(ExamRecord.class))).thenAnswer(invocation -> {
      ExamRecord r = invocation.getArgument(0);
      r.setId(1L);
      return 1;
    });

    // When
    ExamRecord result = examService.beginExam(studentId, taskId);

    // Then
    assertNotNull(result);
    assertEquals("IN_PROGRESS", result.getStatus());
    assertNotNull(result.getStartTime());
    verify(examRecordMapper).insert(any(ExamRecord.class));
  }

  @Test
  void beginExam_shouldReturnExistingRecord_whenInProgress() {
    // Given
    Long studentId = 5L;
    Long taskId = 60L;

    when(examRecordMapper.findByStudentAndTask(studentId, taskId)).thenReturn(testRecord);

    // When
    ExamRecord result = examService.beginExam(studentId, taskId);

    // Then
    assertNotNull(result);
    assertEquals("IN_PROGRESS", result.getStatus());
    verify(examRecordMapper, never()).insert(any(ExamRecord.class));
  }

  // ========== 暂存考试进度（修复版） ==========
  @Test
  void saveProgress_shouldUpdateContent_whenRecordExists() {
    // Given
    Long studentId = 5L;
    Long taskId = 60L;
    String newContent = "{\"answers\":{\"1\":\"B\",\"2\":\"已修改简答\"}}";

    when(examRecordMapper.findByStudentAndTask(studentId, taskId)).thenReturn(testRecord);
    // ✅ 修复：updateContent 返回 int，用 thenReturn
    when(examRecordMapper.updateContent(any(ExamRecord.class))).thenReturn(1);

    // When
    ExamRecord result = examService.saveProgress(studentId, taskId, newContent);

    // Then
    assertNotNull(result);
    assertEquals(newContent, result.getContent());
    verify(examRecordMapper).updateContent(testRecord);
  }

  @Test
  void saveProgress_shouldReturnNull_whenRecordSubmitted() {
    // Given
    Long studentId = 5L;
    Long taskId = 60L;
    testRecord.setStatus("SUBMITTED");

    when(examRecordMapper.findByStudentAndTask(studentId, taskId)).thenReturn(testRecord);

    // When
    ExamRecord result = examService.saveProgress(studentId, taskId, "new content");

    // Then
    assertNotNull(result);
    verify(examRecordMapper, never()).updateContent(any(ExamRecord.class));
  }

  // ========== 提交考试 ==========
  @Test
  void submitExam_shouldCreateRecord_whenNoExistingRecord() {
    // Given
    Long studentId = 5L;
    Long taskId = 60L;
    String content = "考试答案";

    when(examRecordMapper.findByStudentAndTask(studentId, taskId)).thenReturn(null);
    when(examRecordMapper.insert(any(ExamRecord.class))).thenAnswer(invocation -> {
      ExamRecord r = invocation.getArgument(0);
      r.setId(2L);
      return 1;
    });
    when(examRecordMapper.submit(any(ExamRecord.class))).thenReturn(1);

    // When
    ExamRecord result = examService.submitExam(studentId, taskId, content);

    // Then
    assertNotNull(result);
    assertEquals("SUBMITTED", result.getStatus());
    verify(examRecordMapper).insert(any(ExamRecord.class));
    verify(examRecordMapper).submit(any(ExamRecord.class));
  }

  @Test
  void submitExam_shouldReturnExistingRecord_whenAlreadySubmitted() {
    // Given
    Long studentId = 5L;
    Long taskId = 60L;
    testRecord.setStatus("SUBMITTED");

    when(examRecordMapper.findByStudentAndTask(studentId, taskId)).thenReturn(testRecord);

    // When
    ExamRecord result = examService.submitExam(studentId, taskId, "new content");

    // Then
    assertNotNull(result);
    assertEquals("SUBMITTED", result.getStatus());
    verify(examRecordMapper, never()).insert(any(ExamRecord.class));
    verify(examRecordMapper, never()).submit(any(ExamRecord.class));
  }
}
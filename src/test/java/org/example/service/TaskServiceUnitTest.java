package org.example.service;

import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.mapper.SubmissionMapper;
import org.example.mapper.TaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceUnitTest {

  @Mock
  private TaskMapper taskMapper;

  @Mock
  private SubmissionMapper submissionMapper;

  @InjectMocks
  private TaskService taskService;

  private Task testTask;
  private Submission testSubmission;

  @BeforeEach
  void setUp() {
    testTask = new Task();
    testTask.setId(50L);
    testTask.setTitle("两数之和");
    testTask.setType("programming");
    testTask.setMaxScore(100);
    testTask.setStatus("published");
    testTask.setCourseId(10L);

    testSubmission = new Submission();
    testSubmission.setId(1L);
    testSubmission.setTaskId(50L);
    testSubmission.setStudentId(5L);
    testSubmission.setContent("第一次提交内容");
    testSubmission.setStatus("submitted");
  }

  // ========== T030: 作业提交覆盖更新逻辑（正例） ==========
  @Test
  void submit_shouldUpdateExistingSubmission_whenAlreadySubmitted() {
    // Given
    Long taskId = 50L;
    Long studentId = 5L;
    String newContent = "修改后的提交内容";

    when(submissionMapper.findByStudentAndTask(studentId, taskId))
        .thenReturn(testSubmission);
    // ✅ 修复：updateContent 返回 int，用 thenReturn
    when(submissionMapper.updateContent(any(Submission.class))).thenReturn(1);

    // When
    Submission result = taskService.submit(taskId, studentId, newContent);

    // Then
    assertNotNull(result);
    assertEquals(newContent, result.getContent());
    assertEquals(testSubmission.getId(), result.getId());
    verify(submissionMapper).updateContent(testSubmission);
    verify(submissionMapper, never()).insert(any(Submission.class));
  }

  @Test
  void submit_shouldCreateNewSubmission_whenNotSubmitted() {
    // Given
    Long taskId = 50L;
    Long studentId = 6L;
    String content = "第一次提交内容";

    when(submissionMapper.findByStudentAndTask(studentId, taskId)).thenReturn(null);
    when(submissionMapper.insert(any(Submission.class))).thenAnswer(invocation -> {
      Submission s = invocation.getArgument(0);
      s.setId(2L);
      return 1;
    });

    // When
    Submission result = taskService.submit(taskId, studentId, content);

    // Then
    assertNotNull(result);
    assertEquals(content, result.getContent());
    assertEquals("submitted", result.getStatus());
    verify(submissionMapper).insert(any(Submission.class));
  }

  // ========== 任务状态更新 ==========
  @Test
  void updateStatus_shouldCallMapper() {
    // Given
    Long taskId = 50L;
    String status = "retracted";

    // ✅ 修复：updateStatus 返回 int，用 thenReturn
    when(taskMapper.updateStatus(taskId, status)).thenReturn(1);

    // When
    taskService.updateStatus(taskId, status);

    // Then
    verify(taskMapper).updateStatus(taskId, status);
  }
}
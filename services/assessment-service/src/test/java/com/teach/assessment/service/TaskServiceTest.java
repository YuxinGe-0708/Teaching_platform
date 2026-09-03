package com.teach.assessment.service;

import com.teach.assessment.entity.Submission;
import com.teach.assessment.entity.Task;
import com.teach.assessment.mapper.SubmissionMapper;
import com.teach.assessment.mapper.TaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private SubmissionMapper submissionMapper;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = new Task();
        sampleTask.setId(100L);
        sampleTask.setTitle("实验一：微服务拆分");
        sampleTask.setCourseId(10L);
    }

    @Test
    @DisplayName("UNIT-TC-TASK-01: 发布作业（状态为空时默认 published）")
    void testCreateTask_DefaultStatus() {
        when(taskMapper.insert(any(Task.class))).thenReturn(1);

        Task created = taskService.createTask(sampleTask);

        assertNotNull(created);
        assertEquals("published", created.getStatus());
        verify(taskMapper).insert(sampleTask);
    }

    @Test
    @DisplayName("UNIT-TC-TASK-02: 查询课程任务（自动去重过滤重复 Task ID）")
    void testGetCourseTasks_Distinct() {
        Task t1 = new Task(); t1.setId(101L);
        Task t2 = new Task(); t2.setId(101L); // 重复 ID
        Task t3 = new Task(); t3.setId(102L);

        when(taskMapper.findByCourseId(10L)).thenReturn(Arrays.asList(t1, t2, t3));

        List<Task> result = taskService.getCourseTasks(10L);

        assertEquals(2, result.size());
        assertEquals(101L, result.get(0).getId());
        assertEquals(102L, result.get(1).getId());
    }

    @Test
    @DisplayName("UNIT-TC-TASK-03: 首次提交作业（插入新提交记录）")
    void testSubmit_FirstTime() {
        when(submissionMapper.findByStudentAndTask(1L, 100L)).thenReturn(null);
        when(submissionMapper.insert(any(Submission.class))).thenReturn(1);

        Submission sub = taskService.submit(100L, 1L, "这是我的作业代码内容");

        assertNotNull(sub);
        assertEquals(100L, sub.getTaskId());
        assertEquals(1L, sub.getStudentId());
        assertEquals("submitted", sub.getStatus());
        verify(submissionMapper).insert(any(Submission.class));
        verify(submissionMapper, never()).updateContent(any(Submission.class));
    }

    @Test
    @DisplayName("UNIT-TC-TASK-04: 再次提交作业（覆盖更新已有内容，不重复插入）")
    void testSubmit_UpdateExisting() {
        Submission existing = new Submission();
        existing.setId(50L);
        existing.setContent("旧版本");

        when(submissionMapper.findByStudentAndTask(1L, 100L)).thenReturn(existing);
        when(submissionMapper.updateContent(any(Submission.class))).thenReturn(1);

        Submission sub = taskService.submit(100L, 1L, "新版本代码");

        assertEquals(50L, sub.getId());
        assertEquals("新版本代码", existing.getContent());
        verify(submissionMapper).updateContent(existing);
        verify(submissionMapper, never()).insert(any(Submission.class));
    }

    @Test
    @DisplayName("UNIT-TC-TASK-05: 任务更新、状态流转与删除操作")
    void testTaskLifecycle() {
        when(taskMapper.findById(100L)).thenReturn(sampleTask);

        taskService.updateStatus(100L, "closed");
        verify(taskMapper).updateStatus(100L, "closed");

        taskService.deleteTask(100L);
        verify(taskMapper).delete(100L);

        assertNotNull(taskService.findById(100L));
    }
}
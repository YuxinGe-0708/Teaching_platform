package com.teach.assessment.service;

import com.teach.assessment.entity.ExamRecord;
import com.teach.assessment.entity.Submission;
import com.teach.assessment.entity.Task;
import com.teach.assessment.mapper.ExamRecordMapper;
import com.teach.assessment.mapper.SubmissionMapper;
import com.teach.assessment.util.TaskMetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

  @Mock
  private ExamRecordMapper examRecordMapper;

  @Mock
  private SubmissionMapper submissionMapper;

  @InjectMocks
  private ExamService examService;

  private Task examTask;

  @BeforeEach
  void setUp() {
    examTask = new Task();
    examTask.setId(200L);
    examTask.setMaxScore(100);
    // 构造带有标准答案元数据的 description
    examTask.setDescription(TaskMetadataUtils.buildDescription("期中考试", "B", "", "python", ""));
  }

  @Test
  @DisplayName("UNIT-TC-EXAM-01: 学生开始考试（首次进入创建 IN_PROGRESS 记录）")
  void testBeginExam_FirstTime() {
    when(examRecordMapper.findByStudentAndTask(1L, 200L)).thenReturn(null);
    when(examRecordMapper.insert(any(ExamRecord.class))).thenReturn(1);

    ExamRecord record = examService.beginExam(1L, 200L);

    assertNotNull(record);
    assertEquals("IN_PROGRESS", record.getStatus());
    assertNotNull(record.getStartTime());
    verify(examRecordMapper).insert(any(ExamRecord.class));
  }

  @Test
  @DisplayName("UNIT-TC-EXAM-02: 开始考试（已开始或已交卷时保持原记录状态）")
  void testBeginExam_AlreadyStartedOrSubmitted() {
    ExamRecord inProgressRecord = new ExamRecord();
    inProgressRecord.setStatus("IN_PROGRESS");
    when(examRecordMapper.findByStudentAndTask(1L, 200L)).thenReturn(inProgressRecord);

    ExamRecord res1 = examService.beginExam(1L, 200L);
    assertEquals("IN_PROGRESS", res1.getStatus());
    verify(examRecordMapper, never()).insert(any(ExamRecord.class));

    ExamRecord submittedRecord = new ExamRecord();
    submittedRecord.setStatus("SUBMITTED");
    when(examRecordMapper.findByStudentAndTask(2L, 200L)).thenReturn(submittedRecord);

    ExamRecord res2 = examService.beginExam(2L, 200L);
    assertEquals("SUBMITTED", res2.getStatus());
  }

  @Test
  @DisplayName("UNIT-TC-EXAM-03: 保存作答进度（已交卷时禁止修改）")
  void testSaveProgress() {
    ExamRecord inProgressRecord = new ExamRecord();
    inProgressRecord.setStatus("IN_PROGRESS");
    when(examRecordMapper.findByStudentAndTask(1L, 200L)).thenReturn(inProgressRecord);

    ExamRecord saved = examService.saveProgress(1L, 200L, "最新作答内容");
    assertEquals("最新作答内容", saved.getContent());
    verify(examRecordMapper).updateContent(inProgressRecord);

    // 已交卷状态禁止修改
    ExamRecord submittedRecord = new ExamRecord();
    submittedRecord.setStatus("SUBMITTED");
    submittedRecord.setContent("原提交内容");
    when(examRecordMapper.findByStudentAndTask(2L, 200L)).thenReturn(submittedRecord);

    ExamRecord rejected = examService.saveProgress(2L, 200L, "篡改内容");
    assertEquals("原提交内容", rejected.getContent());
  }

  @Test
  @DisplayName("UNIT-TC-EXAM-04: 学生主动交卷与超时自动交卷状态流转")
  void testSubmitExam_Statuses() {
    ExamRecord record = new ExamRecord();
    record.setStatus("IN_PROGRESS");
    when(examRecordMapper.findByStudentAndTask(1L, 200L)).thenReturn(record);

    // 主动交卷
    ExamRecord sub1 = examService.submitExam(1L, 200L, "我的答案");
    assertEquals("SUBMITTED", sub1.getStatus());
    verify(examRecordMapper).submit(record);

    // 自动交卷
    record.setStatus("IN_PROGRESS");
    ExamRecord sub2 = examService.autoSubmitExam(1L, 200L, "超时交卷答案");
    assertEquals("AUTO_SUBMITTED", sub2.getStatus());
  }

  @Test
  @DisplayName("UNIT-TC-EXAM-05: 考试倒计时与超时判定")
  void testRemainingSecondsAndTimeUp() {
    ExamRecord record = new ExamRecord();
    record.setStartTime(new Timestamp(System.currentTimeMillis()));

    // 设置截止时间为未来 100 秒
    examTask.setEndTime(new Timestamp(System.currentTimeMillis() + 100_000L));
    long remaining = examService.getRemainingSeconds(record, examTask);
    assertTrue(remaining > 90 && remaining <= 100);
    assertFalse(examService.isExamTimeUp(record, examTask));

    // 设置截止时间为过去
    examTask.setEndTime(new Timestamp(System.currentTimeMillis() - 10_000L));
    assertEquals(0, examService.getRemainingSeconds(record, examTask));
    assertTrue(examService.isExamTimeUp(record, examTask));
  }

  @Test
  @DisplayName("UNIT-TC-EXAM-06: 自动判分（答案正确给满分 100，判题结果 AC）")
  void testCreateSubmissionFromExam_CorrectAnswer() {
    ExamRecord record = new ExamRecord();
    record.setStudentId(1L);
    record.setTaskId(200L);
    // 作答为标准答案 "B"
    record.setContent("{\"answers\":{\"1\":\"B\"}}");

    when(submissionMapper.findByStudentAndTask(1L, 200L)).thenReturn(null);

    examService.createSubmissionFromExam(record, examTask);

    verify(submissionMapper).insert(any(Submission.class));
    verify(submissionMapper).grade(argThat(sub ->
        sub.getScore() == 100.0 && "AC".equals(sub.getJudgeResult()) && "graded".equals(sub.getStatus())
    ));
    assertEquals(100.0, record.getScore());
  }

  @Test
  @DisplayName("UNIT-TC-EXAM-07: 自动判分（答案错误给 0 分，判题结果 WA）")
  void testCreateSubmissionFromExam_WrongAnswer() {
    ExamRecord record = new ExamRecord();
    record.setStudentId(1L);
    record.setTaskId(200L);
    // 作答为错误答案 "C"
    record.setContent("{\"answers\":{\"1\":\"C\"}}");

    when(submissionMapper.findByStudentAndTask(1L, 200L)).thenReturn(null);

    examService.createSubmissionFromExam(record, examTask);

    verify(submissionMapper).grade(argThat(sub ->
        sub.getScore() == 0.0 && "WA".equals(sub.getJudgeResult())
    ));
    assertEquals(0.0, record.getScore());
  }
}
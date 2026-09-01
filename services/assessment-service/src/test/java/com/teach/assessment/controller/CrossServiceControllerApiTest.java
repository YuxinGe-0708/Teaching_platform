package com.teach.assessment.controller;

import com.teach.assessment.entity.ExamRecord;
import com.teach.assessment.entity.Submission;
import com.teach.assessment.entity.Task;
import com.teach.assessment.mapper.ExamRecordMapper;
import com.teach.assessment.mapper.SubmissionMapper;
import com.teach.assessment.service.ExamService;
import com.teach.assessment.service.LearningServiceClient;
import com.teach.assessment.service.TaskService;
import com.teach.assessment.service.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Exercises assessment -> learning/user calls and the main, alternate and error controller flows. */
class CrossServiceControllerApiTest {
    private ExamService exams; private TaskService tasks; private ExamRecordMapper examMapper;
    private SubmissionMapper submissions; private LearningServiceClient learning; private UserServiceClient users;
    private MockMvc mvc;

    @BeforeEach void setup(){
        exams=mock(ExamService.class);tasks=mock(TaskService.class);examMapper=mock(ExamRecordMapper.class);
        submissions=mock(SubmissionMapper.class);learning=mock(LearningServiceClient.class);users=mock(UserServiceClient.class);
        mvc=MockMvcBuilders.standaloneSetup(new ExamController(exams,tasks,examMapper,learning),
                new SubmissionController(submissions,tasks,learning,users)).build();
    }

    @Test void examMainAutoSubmitAndAuthorizationFailures() throws Exception {
        Task exam=task("exam"); ExamRecord record=record("IN_PROGRESS");
        when(tasks.findById(1L)).thenReturn(exam);when(learning.enrolled(10L,3L)).thenReturn(true);
        when(exams.getExamRecord(3L,1L)).thenReturn(record);when(exams.beginExam(3L,1L)).thenReturn(record);
        when(exams.saveProgress(3L,1L,"answer")).thenReturn(record);
        when(exams.submitExam(3L,1L,"answer")).thenReturn(record("SUBMITTED"));
        when(exams.autoSubmitExam(3L,1L,"answer")).thenReturn(record("AUTO_SUBMITTED"));
        mvc.perform(get("/internal/exams/1/student/3")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/internal/exams/1/begin").param("studentId","3")).andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        mvc.perform(put("/internal/exams/1/progress").param("studentId","3").param("content","answer")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/internal/exams/1/submit").param("studentId","3").param("content","answer")).andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        mvc.perform(post("/internal/exams/1/submit").param("studentId","3").param("content","answer").param("auto","true")).andExpect(jsonPath("$.data.status").value("AUTO_SUBMITTED"));
        verify(exams,times(2)).createSubmissionFromExam(any(ExamRecord.class),eq(exam));

        when(tasks.findById(99L)).thenReturn(null);
        mvc.perform(post("/internal/exams/99/begin").param("studentId","3")).andExpect(jsonPath("$.code").value(404));
        when(tasks.findById(1L)).thenReturn(exam);when(learning.enrolled(10L,4L)).thenReturn(false);
        mvc.perform(post("/internal/exams/1/begin").param("studentId","4")).andExpect(jsonPath("$.code").value(403));
        mvc.perform(put("/internal/exams/1/progress").param("studentId","4")).andExpect(jsonPath("$.code").value(403));
        mvc.perform(post("/internal/exams/1/submit").param("studentId","4")).andExpect(jsonPath("$.code").value(403));
    }

    @Test void submissionCrudPersistsAndGradingNotifiesUserService() throws Exception {
        Task homework=task("homework"); Submission s=submission();
        when(tasks.findById(1L)).thenReturn(homework);when(learning.enrolled(10L,3L)).thenReturn(true);
        when(tasks.submit(1L,3L,"answer")).thenReturn(s);when(submissions.findById(5L)).thenReturn(s);
        when(submissions.findByTaskId(1L)).thenReturn(Collections.singletonList(s));when(submissions.findByStudentId(3L)).thenReturn(Collections.singletonList(s));
        mvc.perform(get("/internal/submissions/task/1")).andExpect(jsonPath("$.data[0].id").value(5));
        mvc.perform(get("/internal/submissions/student/3")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/internal/submissions/5")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/internal/submissions").param("taskId","1").param("studentId","3").param("content","answer").param("filePath","a.txt")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/internal/submissions/5").param("content","revised")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/internal/submissions/5/grade").param("score","88").param("feedback","good")).andExpect(jsonPath("$.data.status").value("graded"));
        verify(submissions).grade(s);verify(users).notify(3L,"成绩已发布","good");verify(users).log(3L,"提交批改","submissionId=5");

        when(tasks.findById(99L)).thenReturn(null);when(submissions.findById(99L)).thenReturn(null);
        mvc.perform(post("/internal/submissions").param("taskId","99").param("studentId","3")).andExpect(jsonPath("$.code").value(404));
        when(tasks.findById(1L)).thenReturn(homework);when(learning.enrolled(10L,4L)).thenReturn(false);
        mvc.perform(post("/internal/submissions").param("taskId","1").param("studentId","4")).andExpect(jsonPath("$.code").value(403));
        mvc.perform(get("/internal/submissions/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/internal/submissions/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(post("/internal/submissions/99/grade").param("score","1")).andExpect(jsonPath("$.code").value(404));
    }

    private Task task(String type){Task t=new Task();t.setId(1L);t.setCourseId(10L);t.setType(type);return t;}
    private ExamRecord record(String status){ExamRecord r=new ExamRecord();r.setId(7L);r.setTaskId(1L);r.setStudentId(3L);r.setStatus(status);return r;}
    private Submission submission(){Submission s=new Submission();s.setId(5L);s.setTaskId(1L);s.setStudentId(3L);s.setStatus("submitted");return s;}
}

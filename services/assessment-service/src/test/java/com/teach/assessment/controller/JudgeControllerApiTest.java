package com.teach.assessment.controller;

import com.teach.assessment.entity.Submission;
import com.teach.assessment.entity.Task;
import com.teach.assessment.mapper.SubmissionMapper;
import com.teach.assessment.service.JudgeService;
import com.teach.assessment.service.TaskService;
import com.teach.assessment.util.TaskMetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JudgeControllerApiTest {
    private JudgeService judge;
    private TaskService tasks;
    private SubmissionMapper submissions;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        judge = mock(JudgeService.class);
        tasks = mock(TaskService.class);
        submissions = mock(SubmissionMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new JudgeController(judge, tasks, submissions)).build();
    }

    @Test
    void judgedSubmissionIsStoredForTheIdentityProvidedByTheBff() throws Exception {
        Task task = new Task();
        task.setId(2L);
        task.setType("programming");
        task.setDescription(TaskMetadataUtils.buildDescription(
                "sum", null, "---CASE---\n1 2\n---OUTPUT---\n3", "python", null));
        when(tasks.findById(2L)).thenReturn(task);
        Submission submission = new Submission();
        submission.setId(10L);
        when(tasks.submit(2L, 9L, "print(3)")).thenReturn(submission);
        JudgeService.JudgeResult result = new JudgeService.JudgeResult();
        result.status = "AC";
        result.score = 100;
        result.passedCases = 1;
        result.totalCases = 1;
        result.usedLocalJudge = true;
        when(judge.judge(any(), any(), any())).thenReturn(result);

        mvc.perform(post("/api/v2/judge/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":2,\"studentId\":9,\"language\":\"python\",\"code\":\"print(3)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("AC"))
                .andExpect(jsonPath("$.data.usedLocalJudge").value(true));

        verify(tasks).submit(2L, 9L, "print(3)");
        verify(submissions).grade(submission);
    }

    @Test
    void rejectsEmptyCodeMissingTaskAndDisallowedLanguage() throws Exception {
        mvc.perform(post("/api/v2/judge/submit").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\" \"}"))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/v2/judge/submit").contentType(MediaType.APPLICATION_JSON).content("{\"taskId\":99,\"code\":\"print(1)\"}"))
                .andExpect(jsonPath("$.code").value(404));

        Task task = new Task(); task.setId(2L); task.setType("programming");
        task.setDescription(TaskMetadataUtils.buildDescription("sum", null, "---CASE---\n1\n---OUTPUT---\n1", "java", null));
        when(tasks.findById(2L)).thenReturn(task);
        mvc.perform(post("/api/v2/judge/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":2,\"language\":\"python\",\"code\":\"print(1)\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}

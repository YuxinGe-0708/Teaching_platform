package com.teach.assessment.controller;

import com.teach.assessment.entity.Task;
import com.teach.assessment.service.LearningServiceClient;
import com.teach.assessment.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Collections;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerApiTest {
    private TaskService service; private LearningServiceClient learning; private MockMvc mvc;
    @BeforeEach void setup(){service=mock(TaskService.class);learning=mock(LearningServiceClient.class);mvc=MockMvcBuilders.standaloneSetup(new TaskController(service,learning)).build();}
    @Test void listUsesAssessmentOwnedTaskRepository() throws Exception {when(service.getCourseTasks(2L)).thenReturn(Collections.emptyList());mvc.perform(get("/internal/tasks").param("courseId","2").header("X-Internal-Api-Key","test")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));verify(service).getCourseTasks(2L);}
    @Test void createChecksCourseThroughLearningApiClient() throws Exception {when(learning.courseExists(2L)).thenReturn(false);mvc.perform(post("/internal/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":2,\"title\":\"Exam\",\"type\":\"exam\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));verify(service,never()).createTask(any());}
}

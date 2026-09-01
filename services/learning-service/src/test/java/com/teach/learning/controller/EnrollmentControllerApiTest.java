package com.teach.learning.controller;

import com.teach.learning.entity.CourseEnrollment;
import com.teach.learning.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EnrollmentControllerApiTest {
    private EnrollmentService service; private MockMvc mvc;
    @BeforeEach void setup(){service=mock(EnrollmentService.class);mvc=MockMvcBuilders.standaloneSetup(new EnrollmentController(service)).build();}
    @Test void enrollReturnsOwnedLearningData() throws Exception { CourseEnrollment e=new CourseEnrollment();e.setId(5L);e.setStudentId(9L);e.setCourseId(2L);when(service.enroll(9L,2L,null)).thenReturn(e);
        mvc.perform(post("/api/enrollments").param("studentId","9").param("courseId","2")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.courseId").value(2)); }
    @Test void dropReportsMissingEnrollment() throws Exception {when(service.unenroll(9L,2L)).thenReturn(false);mvc.perform(delete("/api/enrollments").param("studentId","9").param("courseId","2")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));}
}

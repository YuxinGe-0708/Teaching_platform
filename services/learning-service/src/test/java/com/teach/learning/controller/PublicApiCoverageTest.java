package com.teach.learning.controller;

import com.teach.learning.entity.*;
import com.teach.learning.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MockMvc contract coverage for every public learning-service endpoint. */
class PublicApiCoverageTest {
    private AiService ai;
    private CourseService courses;
    private CourseClassService classes;
    private EnrollmentService enrollments;
    private ResourceService resources;
    private ResourceProgressService progress;
    private StudyNoteService notes;
    private DiscussionService discussions;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ai=mock(AiService.class); courses=mock(CourseService.class); classes=mock(CourseClassService.class);
        enrollments=mock(EnrollmentService.class); resources=mock(ResourceService.class);
        progress=mock(ResourceProgressService.class); notes=mock(StudyNoteService.class); discussions=mock(DiscussionService.class);
        mvc=MockMvcBuilders.standaloneSetup(new AiController(ai),new CourseController(courses),new ClassController(classes),
                new EnrollmentController(enrollments),new ResourceController(resources),new ResourceProgressController(progress),
                new StudyNoteController(notes),new DiscussionController(discussions),new VersionController()).build();
    }

    @Test
    void aiEndpointsCoverSuccessDefaultsAndInvalidMessage() throws Exception {
        when(ai.chat(anyString(),anyString(),anyString())).thenReturn("reply");
        when(ai.explainImage(anyString(),anyString(),anyString())).thenReturn("image reply");
        when(ai.summarize(anyString(),anyString(),anyString())).thenReturn("summary");
        when(ai.mindMap(anyString(),anyString(),anyString())).thenReturn("mindmap");
        mvc.perform(post("/api/v2/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hello\"}"))
                .andExpect(jsonPath("$.data.reply").value("reply"));
        mvc.perform(post("/api/v2/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\" \"}"))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/v2/ai/clear").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/v2/ai/explain-image").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(jsonPath("$.data.reply").value("image reply"));
        mvc.perform(post("/api/v2/ai/summarize").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(jsonPath("$.data").value("summary"));
        mvc.perform(post("/api/v2/ai/mind-map").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(jsonPath("$.data").value("mindmap"));
    }

    @Test
    void courseAndClassCrudCoverEveryRouteAndFailureBranches() throws Exception {
        Course course=course(); CourseClass cc=courseClass();
        when(courses.findById(1L)).thenReturn(course); when(courses.getTeacherCourses(2L)).thenReturn(Collections.singletonList(course));
        when(courses.getStudentCourses(3L)).thenReturn(Collections.singletonList(course)); when(courses.create(anyLong(),anyString(),anyString(),nullable(String.class),anyInt(),anyString(),anyInt(),anyBoolean(),anyString())).thenReturn(course);
        when(courses.update(eq(1L),eq(2L),anyString(),anyString(),nullable(String.class),anyInt(),anyString(),anyInt(),nullable(Boolean.class),nullable(String.class))).thenReturn(true);
        when(courses.archive(1L,2L)).thenReturn(true);
        mvc.perform(get("/api/courses/1")).andExpect(jsonPath("$.data.id").value(1));
        mvc.perform(get("/api/courses").param("teacherId","2")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/courses").param("studentId","3")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/courses")).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/courses").param("teacherId","2").param("name","Course")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/api/courses/1").param("teacherId","2").param("name","Course")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/api/courses/1").param("teacherId","2")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/courses/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/courses/99").param("teacherId","2").param("name","Course")).andExpect(jsonPath("$.code").value(403));
        mvc.perform(delete("/api/courses/99").param("teacherId","2")).andExpect(jsonPath("$.code").value(403));

        when(classes.findById(1L)).thenReturn(cc); when(classes.findByCourseId(1L)).thenReturn(Collections.singletonList(cc));
        when(classes.create(1L,"Class",30)).thenReturn(cc); when(classes.update(1L,"New",40)).thenReturn(true); when(classes.delete(1L)).thenReturn(true);
        mvc.perform(get("/api/classes/1")).andExpect(jsonPath("$.data.id").value(1));
        mvc.perform(get("/api/classes").param("courseId","1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/classes").param("courseId","1").param("name","Class").param("maxCount","30")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/api/classes/1").param("name","New").param("maxCount","40")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/api/classes/1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/classes/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/classes/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(delete("/api/classes/99")).andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void enrollmentRoutesCoverSuccessDuplicateViewAndFailures() throws Exception {
        CourseEnrollment e=enrollment(); when(enrollments.getStudentEnrollments(3L)).thenReturn(Collections.singletonList(e));
        when(enrollments.getCourseEnrollments(1L)).thenReturn(Collections.singletonList(e)); when(enrollments.isEnrolled(3L,1L)).thenReturn(true);
        when(enrollments.enroll(3L,1L,null)).thenReturn(e); when(enrollments.unenroll(3L,1L)).thenReturn(true);
        mvc.perform(get("/api/enrollments/student/3")).andExpect(jsonPath("$.data[0].courseId").value(1));
        mvc.perform(get("/api/enrollments/course/1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/enrollments/check").param("studentId","3").param("courseId","1")).andExpect(jsonPath("$.data").value(true));
        mvc.perform(post("/api/enrollments").param("studentId","3").param("courseId","1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/api/enrollments").param("studentId","3").param("courseId","1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/enrollments").param("studentId","3").param("courseId","99")).andExpect(jsonPath("$.code").value(400));
        mvc.perform(delete("/api/enrollments").param("studentId","3").param("courseId","99")).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void resourceProgressAndNoteRoutesCoverCrudAndMissingData() throws Exception {
        Resource r=resource(); StudyNote n=note(); ResourceProgress rp=new ResourceProgress(); rp.setProgress(50d);rp.setLastPosition(5d);rp.setDuration(10d);
        when(resources.findById(1L)).thenReturn(r); when(resources.findByCourseId(1L)).thenReturn(Collections.singletonList(r));
        when(resources.create(anyLong(),anyString(),nullable(String.class),nullable(String.class),nullable(String.class),nullable(Long.class))).thenReturn(r);
        when(resources.update(eq(1L),anyString(),nullable(String.class),nullable(String.class),nullable(String.class))).thenReturn(true); when(resources.delete(1L)).thenReturn(true);
        mvc.perform(get("/api/resources/1")).andExpect(jsonPath("$.data.downloadCount").value(1));
        mvc.perform(get("/api/resources").param("courseId","1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/resources").param("courseId","1").param("title","R")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/api/resources/1").param("title","R2")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/api/resources/1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/resources/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/resources/99").param("title","x")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(delete("/api/resources/99")).andExpect(jsonPath("$.code").value(404));

        when(progress.findByStudentAndResource(3L,1L)).thenReturn(rp);
        mvc.perform(get("/api/resource-progress").param("studentId","3").param("resourceId","1")).andExpect(jsonPath("$.data.progress").value(50));
        mvc.perform(post("/api/resource-progress").param("studentId","3").param("resourceId","1").param("progress","60")).andExpect(jsonPath("$.code").value(200));
        when(progress.findByStudentAndResource(3L,99L)).thenReturn(null);
        mvc.perform(get("/api/resource-progress").param("studentId","3").param("resourceId","99")).andExpect(jsonPath("$.data.progress").value(0));

        when(notes.findByStudentId(3L)).thenReturn(Collections.singletonList(n)); when(notes.findByStudentAndCourse(3L,1L)).thenReturn(Collections.singletonList(n));
        when(notes.findById(1L)).thenReturn(n); when(notes.create(3L,1L,null,"N","body")).thenReturn(n); when(notes.update(1L,"N2","body2",null,null)).thenReturn(true); when(notes.delete(1L)).thenReturn(true);
        mvc.perform(get("/api/study-notes/student/3")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/study-notes/student/3").param("courseId","1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/study-notes/1")).andExpect(jsonPath("$.data.id").value(1));
        mvc.perform(post("/api/study-notes").param("studentId","3").param("courseId","1").param("title","N").param("content","body")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/api/study-notes/1").param("title","N2").param("content","body2")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/api/study-notes/1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/study-notes/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/study-notes/99").param("title","x").param("content","x")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(delete("/api/study-notes/99")).andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void discussionAndVersionRoutesCoverAllMappingsAndNotFoundAlternatives() throws Exception {
        DiscussionPost p=discussionPost(); DiscussionReply r=reply();
        when(discussions.findByCourseId(1L)).thenReturn(Collections.singletonList(p)); when(discussions.findPostById(1L)).thenReturn(p);
        when(discussions.createPost(anyLong(),anyLong(),anyString(),anyString(),anyBoolean(),anyString(),anyString(),nullable(Long.class))).thenReturn(p);
        when(discussions.deletePost(1L)).thenReturn(true); when(discussions.findRepliesByPostId(1L)).thenReturn(Collections.singletonList(r));
        when(discussions.createReply(anyLong(),anyLong(),anyString(),anyBoolean(),anyBoolean())).thenReturn(r); when(discussions.deleteReply(1L)).thenReturn(true);
        mvc.perform(get("/api/discussions/posts/course/1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/discussions/posts/1")).andExpect(jsonPath("$.data.id").value(1));
        mvc.perform(post("/api/discussions/posts").param("courseId","1").param("userId","3").param("title","Q").param("content","Body")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/api/discussions/posts/1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/discussions/replies/1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/discussions/replies").param("postId","1").param("userId","2").param("content","A")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/api/discussions/replies/1")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/discussions/posts/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(delete("/api/discussions/posts/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(delete("/api/discussions/replies/99")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(get("/api/version")).andExpect(jsonPath("$.data.service").value("learning-service"));
    }

    private Course course(){Course c=new Course();c.setId(1L);c.setTeacherId(2L);c.setName("Course");c.setCode("C1");c.setCredits(3);c.setHours(32);c.setAllowJoin(true);c.setStatus("active");return c;}
    private CourseClass courseClass(){CourseClass c=new CourseClass();c.setId(1L);c.setCourseId(1L);c.setName("Class");c.setMaxCount(30);c.setCurrentCount(1);return c;}
    private CourseEnrollment enrollment(){CourseEnrollment e=new CourseEnrollment();e.setId(1L);e.setStudentId(3L);e.setCourseId(1L);return e;}
    private Resource resource(){Resource r=new Resource();r.setId(1L);r.setCourseId(1L);r.setTitle("R");r.setDownloadCount(0);r.setFileSize(1L);return r;}
    private StudyNote note(){StudyNote n=new StudyNote();n.setId(1L);n.setStudentId(3L);n.setCourseId(1L);n.setTitle("N");n.setContent("body");return n;}
    private DiscussionPost discussionPost(){DiscussionPost p=new DiscussionPost();p.setId(1L);p.setCourseId(1L);p.setUserId(3L);p.setTitle("Q");p.setContent("Body");p.setAnonymous(false);return p;}
    private DiscussionReply reply(){DiscussionReply r=new DiscussionReply();r.setId(1L);r.setPostId(1L);r.setUserId(2L);r.setContent("A");r.setAnonymous(false);r.setAssistantReply(true);return r;}
}

package com.teach.learning.service;

import com.teach.learning.entity.Course;
import com.teach.learning.entity.CourseEnrollment;
import com.teach.learning.mapper.CourseEnrollmentMapper;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnrollmentServiceTest {
    @Test void enrollmentRejectsArchivedCourse() {
        CourseEnrollmentMapper mapper=mock(CourseEnrollmentMapper.class); CourseService courses=mock(CourseService.class); CourseClassService classes=mock(CourseClassService.class);
        Course course=new Course(); course.setStatus("archived"); course.setAllowJoin(true); when(courses.findById(3L)).thenReturn(course);
        assertNull(new EnrollmentService(mapper,courses,classes).enroll(8L,3L,null)); verify(mapper,never()).insert(any());
    }
    @Test void enrollmentIsIdempotent() {
        CourseEnrollmentMapper mapper=mock(CourseEnrollmentMapper.class); CourseService courses=mock(CourseService.class); CourseClassService classes=mock(CourseClassService.class);
        Course course=new Course(); course.setStatus("active"); course.setAllowJoin(true); CourseEnrollment existing=new CourseEnrollment(); existing.setId(11L);
        when(courses.findById(3L)).thenReturn(course); when(mapper.findByStudentAndCourse(8L,3L)).thenReturn(existing); when(classes.findByCourseId(3L)).thenReturn(Collections.emptyList());
        assertSame(existing,new EnrollmentService(mapper,courses,classes).enroll(8L,3L,null)); verify(mapper,never()).insert(any());
    }
}

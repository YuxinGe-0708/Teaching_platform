package com.teach.learning.service;

import com.teach.learning.entity.Course;
import com.teach.learning.entity.CourseClass;
import com.teach.learning.entity.CourseEnrollment;
import com.teach.learning.mapper.CourseEnrollmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private CourseEnrollmentMapper enrollmentMapper;

    @Mock
    private CourseService courseService;

    @Mock
    private CourseClassService classService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Course activeCourse;

    @BeforeEach
    void setUp() {
        activeCourse = new Course();
        activeCourse.setId(10L);
        activeCourse.setStatus("active");
        activeCourse.setAllowJoin(true);
    }

    @Test
    @DisplayName("UNIT-TC-ENROLL-01: 选课成功（指定班级，班级人数自动 +1）")
    void testEnroll_SuccessWithClassId() {
        when(courseService.findById(10L)).thenReturn(activeCourse);
        when(enrollmentMapper.findByStudentAndCourse(1L, 10L)).thenReturn(null);
        when(enrollmentMapper.insert(any(CourseEnrollment.class))).thenReturn(1);

        CourseEnrollment result = enrollmentService.enroll(1L, 10L, 100L);

        assertNotNull(result);
        assertEquals(1L, result.getStudentId());
        assertEquals(10L, result.getCourseId());
        assertEquals(100L, result.getClassId());

        verify(enrollmentMapper).insert(any(CourseEnrollment.class));
        verify(classService).incrementCount(100L); // 班级人数必须自增
    }

    @Test
    @DisplayName("UNIT-TC-ENROLL-02: 选课成功（未指定班级时自动加入默认班级）")
    void testEnroll_SuccessWithDefaultClass() {
        CourseClass defaultClass = new CourseClass();
        defaultClass.setId(888L);

        when(courseService.findById(10L)).thenReturn(activeCourse);
        when(enrollmentMapper.findByStudentAndCourse(1L, 10L)).thenReturn(null);
        when(classService.findByCourseId(10L)).thenReturn(Collections.singletonList(defaultClass));
        when(enrollmentMapper.insert(any(CourseEnrollment.class))).thenReturn(1);

        CourseEnrollment result = enrollmentService.enroll(1L, 10L, null);

        assertNotNull(result);
        assertEquals(888L, result.getClassId());
        verify(classService).incrementCount(888L);
    }

    @Test
    @DisplayName("UNIT-TC-ENROLL-03: 选课失败（课程不存在、非 active 状态或不允许加入）")
    void testEnroll_FailCases() {
        // 课程不存在
        when(courseService.findById(99L)).thenReturn(null);
        assertNull(enrollmentService.enroll(1L, 99L, 100L));

        // 课程已归档
        activeCourse.setStatus("archived");
        when(courseService.findById(10L)).thenReturn(activeCourse);
        assertNull(enrollmentService.enroll(1L, 10L, 100L));

        // 课程关闭选课
        activeCourse.setStatus("active");
        activeCourse.setAllowJoin(false);
        assertNull(enrollmentService.enroll(1L, 10L, 100L));

        verify(enrollmentMapper, never()).insert(any(CourseEnrollment.class));
    }

    @Test
    @DisplayName("UNIT-TC-ENROLL-04: 重复选课返回已有记录（幂等性，不重复插入和增加人数）")
    void testEnroll_AlreadyEnrolled_ReturnsExisting() {
        CourseEnrollment existing = new CourseEnrollment();
        existing.setId(55L);
        when(courseService.findById(10L)).thenReturn(activeCourse);
        when(enrollmentMapper.findByStudentAndCourse(1L, 10L)).thenReturn(existing);

        CourseEnrollment result = enrollmentService.enroll(1L, 10L, 100L);

        assertEquals(55L, result.getId());
        verify(enrollmentMapper, never()).insert(any(CourseEnrollment.class));
        verify(classService, never()).incrementCount(anyLong());
    }

    @Test
    @DisplayName("UNIT-TC-ENROLL-05: 退课成功（班级人数自动 -1 并删除记录）")
    void testUnenroll_Success() {
        CourseEnrollment existing = new CourseEnrollment();
        existing.setClassId(100L);
        when(enrollmentMapper.findByStudentAndCourse(1L, 10L)).thenReturn(existing);
        when(enrollmentMapper.deleteByStudentAndCourse(1L, 10L)).thenReturn(1);

        boolean success = enrollmentService.unenroll(1L, 10L);

        assertTrue(success);
        verify(classService).decrementCount(100L); // 班级人数必须自减
        verify(enrollmentMapper).deleteByStudentAndCourse(1L, 10L);
    }

    @Test
    @DisplayName("UNIT-TC-ENROLL-06: 未选课时退课返回 false")
    void testUnenroll_NotEnrolled_ReturnsFalse() {
        when(enrollmentMapper.findByStudentAndCourse(1L, 10L)).thenReturn(null);

        assertFalse(enrollmentService.unenroll(1L, 10L));
        verify(classService, never()).decrementCount(anyLong());
        verify(enrollmentMapper, never()).deleteByStudentAndCourse(anyLong(), anyLong());
    }

    @Test
    @DisplayName("UNIT-TC-ENROLL-07: 从班级中移除学生")
    void testRemoveFromClass() {
        CourseEnrollment existing = new CourseEnrollment();
        existing.setCourseId(10L);
        existing.setClassId(100L);
        when(enrollmentMapper.findByClassAndStudent(100L, 1L)).thenReturn(existing);
        when(enrollmentMapper.deleteByStudentAndCourse(1L, 10L)).thenReturn(1);

        assertTrue(enrollmentService.removeFromClass(100L, 1L));
        verify(classService).decrementCount(100L);

        // 不存在记录返回 false
        when(enrollmentMapper.findByClassAndStudent(100L, 2L)).thenReturn(null);
        assertFalse(enrollmentService.removeFromClass(100L, 2L));
    }

    @Test
    @DisplayName("UNIT-TC-ENROLL-08: 选课状态判断与选课人数统计")
    void testIsEnrolledAndCount() {
        when(enrollmentMapper.findByStudentAndCourse(1L, 10L)).thenReturn(new CourseEnrollment());
        when(enrollmentMapper.findByStudentAndCourse(2L, 10L)).thenReturn(null);
        when(enrollmentMapper.countByCourseId(10L)).thenReturn(42);

        assertTrue(enrollmentService.isEnrolled(1L, 10L));
        assertFalse(enrollmentService.isEnrolled(2L, 10L));
        assertEquals(42, enrollmentService.countByCourseId(10L));
    }
}

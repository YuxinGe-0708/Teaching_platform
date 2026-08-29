package org.example.service;

import org.example.entity.Course;
import org.example.entity.CourseClass;
import org.example.entity.CourseEnrollment;
import org.example.mapper.CourseClassMapper;
import org.example.mapper.CourseEnrollmentMapper;
import org.example.mapper.CourseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceUnitTest {

  @Mock
  private CourseMapper courseMapper;

  @Mock
  private CourseEnrollmentMapper enrollmentMapper;

  @Mock
  private CourseClassMapper courseClassMapper;

  @InjectMocks
  private CourseService courseService;

  private Course testCourse;
  private CourseClass testClass;

  @BeforeEach
  void setUp() {
    testCourse = new Course();
    testCourse.setId(10L);
    testCourse.setName("操作系统原理");
    testCourse.setCode("CS101");
    testCourse.setCredits(3);
    testCourse.setStatus("active");
    testCourse.setAllowJoin(true);
    testCourse.setTeacherId(1L);

    testClass = new CourseClass();
    testClass.setId(1L);
    testClass.setCourseId(10L);
    testClass.setName("默认班级");
    testClass.setInviteCode("A8B7C9D0");
    testClass.setMaxCount(100);
    testClass.setCurrentCount(0);
  }

  // ========== UC010: 教师创建课程并自动生成邀请码（正例） ==========
  @Test
  void createCourse_shouldGenerateInviteCodeAndDefaultClass() {
    // Given
    Course course = new Course();
    course.setName("操作系统原理");
    course.setCode("CS101");
    course.setCredits(3);

    when(courseMapper.insert(any(Course.class))).thenAnswer(invocation -> {
      Course c = invocation.getArgument(0);
      c.setId(10L);
      return 1;
    });
    when(courseClassMapper.insert(any(CourseClass.class))).thenReturn(1);

    // When
    Course result = courseService.createCourse(course);

    // Then
    assertNotNull(result);
    assertNotNull(result.getInviteCode());
    assertEquals(8, result.getInviteCode().length());
    assertEquals("active", result.getStatus());
    assertTrue(result.getAllowJoin());
    verify(courseClassMapper).insert(any(CourseClass.class));
  }

  // ========== UC030: 学生正常选课逻辑（正例） ==========
  @Test
  void enroll_shouldReturnTrue_whenCourseActiveAndNotEnrolled() {
    // Given
    Long studentId = 5L;
    Long courseId = 10L;

    when(courseMapper.findById(courseId)).thenReturn(testCourse);
    when(enrollmentMapper.findByStudentAndCourse(studentId, courseId)).thenReturn(null);
    when(courseClassMapper.findByCourseId(courseId)).thenReturn(Arrays.asList(testClass));
    when(enrollmentMapper.insert(any(CourseEnrollment.class))).thenReturn(1);
    when(courseClassMapper.incrementCount(testClass.getId())).thenReturn(1);

    // When
    boolean result = courseService.enroll(studentId, courseId);

    // Then
    assertTrue(result);
    verify(enrollmentMapper).insert(any(CourseEnrollment.class));
    verify(courseClassMapper).incrementCount(testClass.getId());
  }

  // ========== UC031: 学生重复选课拦截（反例） ==========
  @Test
  void enroll_shouldReturnFalse_whenAlreadyEnrolled() {
    // Given
    Long studentId = 5L;
    Long courseId = 10L;

    when(courseMapper.findById(courseId)).thenReturn(testCourse);
    when(enrollmentMapper.findByStudentAndCourse(studentId, courseId))
        .thenReturn(new CourseEnrollment());

    // When
    boolean result = courseService.enroll(studentId, courseId);

    // Then
    assertFalse(result);
    verify(enrollmentMapper, never()).insert(any(CourseEnrollment.class));
  }

  // ========== UC032: 课程禁止加入时选课拦截（反例） ==========
  @Test
  void enroll_shouldReturnFalse_whenCourseNotAllowJoin() {
    // Given
    Long studentId = 5L;
    Long courseId = 12L;
    testCourse.setAllowJoin(false);

    when(courseMapper.findById(courseId)).thenReturn(testCourse);

    // When
    boolean result = courseService.enroll(studentId, courseId);

    // Then
    assertFalse(result);
    verify(enrollmentMapper, never()).insert(any(CourseEnrollment.class));
  }

  // ========== UC060: 使用邀请码加入班级（正例） ==========
  @Test
  void enrollByInviteCode_shouldReturnTrue_whenInviteCodeValid() {
    // Given
    Long studentId = 6L;
    String inviteCode = "CLASS888";

    when(courseClassMapper.findByInviteCode(inviteCode)).thenReturn(testClass);
    when(courseMapper.findById(testClass.getCourseId())).thenReturn(testCourse);
    when(enrollmentMapper.findByStudentAndCourse(studentId, testClass.getCourseId())).thenReturn(null);
    when(enrollmentMapper.insert(any(CourseEnrollment.class))).thenReturn(1);
    when(courseClassMapper.incrementCount(testClass.getId())).thenReturn(1);

    // When
    boolean result = courseService.enrollByInviteCode(studentId, inviteCode);

    // Then
    assertTrue(result);
    verify(enrollmentMapper).insert(any(CourseEnrollment.class));
  }

  // ========== UC061: 使用不存在的邀请码选课失败（反例） ==========
  @Test
  void enrollByInviteCode_shouldReturnFalse_whenInviteCodeInvalid() {
    // Given
    Long studentId = 6L;
    String inviteCode = "NOTEXIST";

    when(courseClassMapper.findByInviteCode(inviteCode)).thenReturn(null);
    when(courseMapper.findByInviteCode(inviteCode)).thenReturn(null);

    // When
    boolean result = courseService.enrollByInviteCode(studentId, inviteCode);

    // Then
    assertFalse(result);
    verify(enrollmentMapper, never()).insert(any(CourseEnrollment.class));
  }

  // ========== 退课测试 ==========
  @Test
  void unenroll_shouldReturnTrue_whenEnrollmentExists() {
    // Given
    Long studentId = 5L;
    Long courseId = 10L;

    when(enrollmentMapper.delete(studentId, courseId)).thenReturn(1);
    when(courseClassMapper.findByCourseId(courseId)).thenReturn(Arrays.asList(testClass));
    when(courseClassMapper.decrementCount(testClass.getId())).thenReturn(1);

    // When
    boolean result = courseService.unenroll(studentId, courseId);

    // Then
    assertTrue(result);
    verify(courseClassMapper).decrementCount(testClass.getId());
  }
}
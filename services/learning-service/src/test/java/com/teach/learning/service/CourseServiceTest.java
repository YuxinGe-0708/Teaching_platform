package com.teach.learning.service;

import com.teach.learning.entity.Course;
import com.teach.learning.mapper.CourseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  @Mock
  private CourseMapper courseMapper;

  @InjectMocks
  private CourseService courseService;

  private Course sampleCourse;

  @BeforeEach
  void setUp() {
    sampleCourse = new Course();
    sampleCourse.setId(10L);
    sampleCourse.setName("软件工程");
    sampleCourse.setCode("SE-101");
    sampleCourse.setTeacherId(100L);
    sampleCourse.setAllowJoin(true);
    sampleCourse.setStatus("active");
    sampleCourse.setInviteCode("ABC12345");
  }

  @Test
  @DisplayName("UNIT-TC-COURSE-01: 创建课程成功（默认状态为 active，自动生成 8 位邀请码）")
  void testCreate_Success() {
    when(courseMapper.insert(any(Course.class))).thenReturn(1);

    Course result = courseService.create(100L, "操作系统", "OS-201", "描述", 3, "计算机", 48, null, "invalid_status");

    assertNotNull(result);
    assertEquals(100L, result.getTeacherId());
    assertEquals("操作系统", result.getName());
    assertTrue(result.getAllowJoin()); // 默认为 true
    assertEquals("active", result.getStatus()); // 非法状态自动归一化为 active
    assertNotNull(result.getInviteCode());
    assertEquals(8, result.getInviteCode().length());
    verify(courseMapper).insert(any(Course.class));
  }

  @Test
  @DisplayName("UNIT-TC-COURSE-02: 创建指定状态的课程（draft / closed / archived）")
  void testCreate_CustomStatus() {
    when(courseMapper.insert(any(Course.class))).thenReturn(1);

    Course draftCourse = courseService.create(100L, "课程", "C1", "描述", 2, "工科", 32, false, "draft");
    assertEquals("draft", draftCourse.getStatus());
    assertFalse(draftCourse.getAllowJoin());
  }

  @Test
  @DisplayName("UNIT-TC-COURSE-03: 课程访问权限判定（教师本人访问）")
  void testCanAccess_TeacherAccess() {
    when(courseMapper.findById(10L)).thenReturn(sampleCourse);

    // 教师本人访问，不论 action 是什么都允许
    assertTrue(courseService.canAccess(10L, 100L, "view"));
    assertTrue(courseService.canAccess(10L, 100L, "edit"));
  }

  @Test
  @DisplayName("UNIT-TC-COURSE-04: 课程访问权限判定（学生选课行为）")
  void testCanAccess_StudentEnrollAction() {
    when(courseMapper.findById(10L)).thenReturn(sampleCourse);

    // 学生选课：状态为 active 且 allowJoin=true -> 允许
    assertTrue(courseService.canAccess(10L, 200L, "enroll"));

    // 课程关闭选课后 -> 拒绝
    sampleCourse.setAllowJoin(false);
    assertFalse(courseService.canAccess(10L, 200L, "enroll"));

    // 课程非 active 状态 -> 拒绝
    sampleCourse.setAllowJoin(true);
    sampleCourse.setStatus("closed");
    assertFalse(courseService.canAccess(10L, 200L, "enroll"));

    // 学生执行非 enroll 动作 -> 拒绝
    assertFalse(courseService.canAccess(10L, 200L, "edit"));
  }

  @Test
  @DisplayName("UNIT-TC-COURSE-05: 课程访问权限判定（课程不存在或用户为空）")
  void testCanAccess_NotFoundOrNullUser() {
    when(courseMapper.findById(99L)).thenReturn(null);

    assertFalse(courseService.canAccess(99L, 100L, "enroll"));
    assertFalse(courseService.canAccess(10L, null, "enroll"));
  }

  @Test
  @DisplayName("UNIT-TC-COURSE-06: 教师更新课程（本人成功更新，非本人拒绝）")
  void testUpdate() {
    when(courseMapper.findById(10L)).thenReturn(sampleCourse);
    when(courseMapper.update(any(Course.class))).thenReturn(1);

    // 教师本人更新 -> 成功
    boolean success = courseService.update(10L, 100L, "新课名", "SE-102", "新描述", 4, "计算机", 64, true, "active");
    assertTrue(success);

    // 非本课教师更新 -> 失败
    boolean fail = courseService.update(10L, 999L, "新课名", "SE-102", "新描述", 4, "计算机", 64, true, "active");
    assertFalse(fail);
  }

  @Test
  @DisplayName("UNIT-TC-COURSE-07: 归档与删除课程权限校验")
  void testArchiveAndDelete() {
    when(courseMapper.findById(10L)).thenReturn(sampleCourse);
    when(courseMapper.deleteById(10L)).thenReturn(1);

    // 归档校验
    assertTrue(courseService.archive(10L, 100L));
    verify(courseMapper).updateStatus(10L, "archived");
    assertFalse(courseService.archive(10L, 999L)); // 非本人拒绝

    // 删除校验
    assertTrue(courseService.delete(10L, 100L));
    assertFalse(courseService.delete(10L, 999L)); // 非本人拒绝
  }

  @Test
  @DisplayName("UNIT-TC-COURSE-08: 课程列表与邀请码查询")
  void testQueries() {
    when(courseMapper.findByTeacherId(100L)).thenReturn(Collections.singletonList(sampleCourse));
    when(courseMapper.findActive()).thenReturn(Collections.singletonList(sampleCourse));
    when(courseMapper.findByInviteCode("ABC12345")).thenReturn(sampleCourse);

    assertEquals(1, courseService.getTeacherCourses(100L).size());
    assertEquals(1, courseService.getActiveCourses().size());
    assertNotNull(courseService.findByInviteCode("ABC12345"));
  }
}

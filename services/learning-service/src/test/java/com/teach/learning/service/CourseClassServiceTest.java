package com.teach.learning.service;

import com.teach.learning.entity.CourseClass;
import com.teach.learning.mapper.CourseClassMapper;
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
class CourseClassServiceTest {

  @Mock
  private CourseClassMapper classMapper;

  @InjectMocks
  private CourseClassService classService;

  @Test
  @DisplayName("UNIT-TC-CLASS-01: 创建班级（默认名称与默认人数 100）")
  void testCreateClass_DefaultValues() {
    when(classMapper.insert(any(CourseClass.class))).thenReturn(1);

    CourseClass cc = classService.create(10L, null, null);

    assertNotNull(cc);
    assertEquals(10L, cc.getCourseId());
    assertEquals("默认班级", cc.getName());
    assertEquals(100, cc.getMaxCount());
    assertEquals(0, cc.getCurrentCount());
    assertNotNull(cc.getInviteCode());
    assertEquals(8, cc.getInviteCode().length());
    verify(classMapper).insert(any(CourseClass.class));
  }

  @Test
  @DisplayName("UNIT-TC-CLASS-02: 更新班级信息（存在与不存在分支）")
  void testUpdateClass() {
    CourseClass existing = new CourseClass();
    existing.setId(5L);
    when(classMapper.findById(5L)).thenReturn(existing);
    when(classMapper.update(any(CourseClass.class))).thenReturn(1);

    // 存在时更新成功
    assertTrue(classService.update(5L, "一班", 50));
    assertEquals("一班", existing.getName());
    assertEquals(50, existing.getMaxCount());

    // 不存在时返回 false
    when(classMapper.findById(99L)).thenReturn(null);
    assertFalse(classService.update(99L, "二班", 50));
  }

  @Test
  @DisplayName("UNIT-TC-CLASS-03: 班级人数增减与删除操作")
  void testCountAndOperations() {
    when(classMapper.deleteById(5L)).thenReturn(1);
    when(classMapper.findByCourseId(10L)).thenReturn(Collections.singletonList(new CourseClass()));

    assertTrue(classService.delete(5L));
    List<CourseClass> list = classService.findByCourseId(10L);
    assertEquals(1, list.size());

    // 验证增减人数调用了 Mapper
    classService.incrementCount(5L);
    verify(classMapper).incrementCount(5L);

    classService.decrementCount(5L);
    verify(classMapper).decrementCount(5L);
  }
}
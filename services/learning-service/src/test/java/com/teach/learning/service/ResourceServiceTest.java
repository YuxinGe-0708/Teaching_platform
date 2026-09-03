package com.teach.learning.service;

import com.teach.learning.entity.Resource;
import com.teach.learning.mapper.ResourceMapper;
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
class ResourceServiceTest {

  @Mock
  private ResourceMapper resourceMapper;

  @InjectMocks
  private ResourceService resourceService;

  @Test
  @DisplayName("UNIT-TC-RES-01: 上传课程资源（空值使用默认值）")
  void testCreate_WithDefaultValues() {
    when(resourceMapper.insert(any(Resource.class))).thenReturn(1);

    Resource r = resourceService.create(10L, "课件1", null, null, null, null);

    assertNotNull(r);
    assertEquals(10L, r.getCourseId());
    assertEquals("课件1", r.getTitle());
    assertEquals("", r.getFilePath());
    assertEquals("other", r.getType());
    assertEquals("默认章节", r.getChapter());
    assertEquals(0L, r.getFileSize());
    assertEquals(0, r.getDownloadCount());
    verify(resourceMapper).insert(any(Resource.class));
  }

  @Test
  @DisplayName("UNIT-TC-RES-02: 更新资源信息（存在时更新，不存在返回 false）")
  void testUpdate_Branches() {
    Resource existing = new Resource();
    existing.setId(1L);
    when(resourceMapper.findById(1L)).thenReturn(existing);
    when(resourceMapper.update(any(Resource.class))).thenReturn(1);

    assertTrue(resourceService.update(1L, "新标题", "/path/new.pdf", "pdf", "第二章"));
    assertEquals("新标题", existing.getTitle());

    when(resourceMapper.findById(99L)).thenReturn(null);
    assertFalse(resourceService.update(99L, "新标题", "/path/new.pdf", "pdf", "第二章"));
  }

  @Test
  @DisplayName("UNIT-TC-RES-03: 资源查询、下载计数与删除")
  void testQueryDownloadAndDelete() {
    when(resourceMapper.findByCourseId(10L)).thenReturn(Collections.singletonList(new Resource()));
    when(resourceMapper.deleteById(1L)).thenReturn(1);

    List<Resource> list = resourceService.findByCourseId(10L);
    assertEquals(1, list.size());

    resourceService.incrementDownloadCount(1L);
    verify(resourceMapper).incrementDownloadCount(1L);

    assertTrue(resourceService.delete(1L));
  }
}

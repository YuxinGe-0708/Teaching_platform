package com.teach.learning.service;

import com.teach.learning.entity.ResourceProgress;
import com.teach.learning.mapper.ResourceProgressMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceProgressServiceTest {

  @Mock
  private ResourceProgressMapper progressMapper;

  @InjectMocks
  private ResourceProgressService progressService;

  @Test
  @DisplayName("UNIT-TC-PROG-01: 首次观看视频时插入新进度记录")
  void testSave_InsertNewProgress() {
    when(progressMapper.findByStudentAndResource(1L, 100L)).thenReturn(null);
    when(progressMapper.insert(any(ResourceProgress.class))).thenReturn(1);

    ResourceProgress rp = progressService.save(1L, 100L, 50.0, 120.5, 240.0);

    assertNotNull(rp);
    assertEquals(1L, rp.getStudentId());
    assertEquals(100L, rp.getResourceId());
    assertEquals(50.0, rp.getProgress());
    verify(progressMapper).insert(any(ResourceProgress.class));
    verify(progressMapper, never()).update(any(ResourceProgress.class));
  }

  @Test
  @DisplayName("UNIT-TC-PROG-02: 再次观看视频时更新已有进度记录")
  void testSave_UpdateExistingProgress() {
    ResourceProgress existing = new ResourceProgress();
    existing.setStudentId(1L);
    existing.setResourceId(100L);
    existing.setProgress(30.0);

    when(progressMapper.findByStudentAndResource(1L, 100L)).thenReturn(existing);
    when(progressMapper.update(any(ResourceProgress.class))).thenReturn(1);

    ResourceProgress rp = progressService.save(1L, 100L, 85.0, 200.0, 240.0);

    assertEquals(85.0, rp.getProgress());
    assertEquals(200.0, rp.getLastPosition());
    verify(progressMapper).update(existing);
    verify(progressMapper, never()).insert(any(ResourceProgress.class));
  }
}
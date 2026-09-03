package com.teach.learning.service;

import com.teach.learning.entity.StudyNote;
import com.teach.learning.mapper.StudyNoteMapper;
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
class StudyNoteServiceTest {

  @Mock
  private StudyNoteMapper noteMapper;

  @InjectMocks
  private StudyNoteService noteService;

  @Test
  @DisplayName("UNIT-TC-NOTE-01: 创建笔记成功")
  void testCreateNote() {
    when(noteMapper.insert(any(StudyNote.class))).thenReturn(1);

    StudyNote note = noteService.create(1L, 10L, 100L, "微服务架构笔记", "包含服务拆分等重点");

    assertNotNull(note);
    assertEquals(1L, note.getStudentId());
    assertEquals("微服务架构笔记", note.getTitle());
    verify(noteMapper).insert(any(StudyNote.class));
  }

  @Test
  @DisplayName("UNIT-TC-NOTE-02: 更新笔记（支持 AI 总结与脑图信息同步更新）")
  void testUpdateNote() {
    StudyNote existing = new StudyNote();
    existing.setId(5L);
    when(noteMapper.findById(5L)).thenReturn(existing);
    when(noteMapper.update(any(StudyNote.class))).thenReturn(1);

    boolean success = noteService.update(5L, "新标题", "新内容", "AI 自动生成的核心概念", "mindmap\n  root");

    assertTrue(success);
    assertEquals("AI 自动生成的核心概念", existing.getAiSummary());
    assertEquals("mindmap\n  root", existing.getMindMap());

    when(noteMapper.findById(99L)).thenReturn(null);
    assertFalse(noteService.update(99L, "标题", "内容", null, null));
  }

  @Test
  @DisplayName("UNIT-TC-NOTE-03: 笔记按课程及按学生查询与删除")
  void testQueryAndDeleteNote() {
    when(noteMapper.findByStudentAndCourse(1L, 10L)).thenReturn(Collections.singletonList(new StudyNote()));
    when(noteMapper.deleteById(5L)).thenReturn(1);

    List<StudyNote> notes = noteService.findByStudentAndCourse(1L, 10L);
    assertEquals(1, notes.size());

    assertTrue(noteService.delete(5L));
  }
}
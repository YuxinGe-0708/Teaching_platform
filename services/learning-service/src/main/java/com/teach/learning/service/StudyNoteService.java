package com.teach.learning.service;
import com.teach.learning.entity.StudyNote;
import com.teach.learning.mapper.StudyNoteMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StudyNoteService {
    private final StudyNoteMapper noteMapper;
    public StudyNoteService(StudyNoteMapper noteMapper) { this.noteMapper = noteMapper; }

    public StudyNote create(Long studentId, Long courseId, Long resourceId, String title, String content) {
        StudyNote note = new StudyNote();
        note.setStudentId(studentId); note.setCourseId(courseId); note.setResourceId(resourceId);
        note.setTitle(title); note.setContent(content);
        noteMapper.insert(note); return note;
    }

    public StudyNote findById(Long id) { return noteMapper.findById(id); }
    public List<StudyNote> findByStudentId(Long studentId) { return noteMapper.findByStudentId(studentId); }
    public List<StudyNote> findByStudentAndCourse(Long studentId, Long courseId) { return noteMapper.findByStudentAndCourse(studentId, courseId); }

    public boolean update(Long id, String title, String content, String aiSummary, String mindMap) {
        StudyNote note = noteMapper.findById(id); if (note == null) return false;
        note.setTitle(title); note.setContent(content); note.setAiSummary(aiSummary); note.setMindMap(mindMap);
        return noteMapper.update(note) > 0;
    }

    public boolean delete(Long id) { return noteMapper.deleteById(id) > 0; }
}

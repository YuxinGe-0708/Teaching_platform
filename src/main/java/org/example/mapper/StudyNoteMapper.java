package org.example.mapper;
import org.example.entity.StudyNote;
import java.util.List;
public interface StudyNoteMapper {
    List<StudyNote> findByStudentId(Long studentId); List<StudyNote> findByStudentAndCourse(Long studentId,Long courseId);
    StudyNote findById(Long id); int insert(StudyNote value); int update(StudyNote value); int updateMindMap(StudyNote value);
    int deleteByStudent(Long id,Long studentId);
}

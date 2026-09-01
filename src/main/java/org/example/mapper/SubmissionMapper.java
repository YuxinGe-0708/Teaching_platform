package org.example.mapper;
import org.example.entity.Submission;
import java.util.List;
public interface SubmissionMapper {
    Submission findById(Long id); List<Submission> findByTaskId(Long taskId); Submission findByStudentAndTask(Long studentId,Long taskId);
    List<Submission> findByStudentId(Long studentId); List<Submission> findByCourseId(Long courseId); int countAll();
    int insert(Submission value); int updateContent(Submission value); int grade(Submission value);
}

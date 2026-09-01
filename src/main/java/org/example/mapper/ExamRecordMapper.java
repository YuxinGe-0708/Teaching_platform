package org.example.mapper;
import org.example.entity.ExamRecord;
import java.util.List;
public interface ExamRecordMapper {
    ExamRecord findById(Long id); ExamRecord findByStudentAndTask(Long studentId,Long taskId);
    List<ExamRecord> findByTaskId(Long taskId); List<ExamRecord> findByStudentId(Long studentId);
    int insert(ExamRecord value); int updateContent(ExamRecord value); int beginExam(ExamRecord value);
    int submit(ExamRecord value); int autoSubmit(ExamRecord value);
}

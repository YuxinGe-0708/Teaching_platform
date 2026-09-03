package com.teach.assessment.mapper;

import org.apache.ibatis.annotations.*;
import com.teach.assessment.entity.ExamRecord;
import java.util.List;

@Mapper
public interface ExamRecordMapper {

    @Select("SELECT * FROM exam_record WHERE id = #{id}")
    ExamRecord findById(Long id);

    @Select("SELECT * FROM exam_record WHERE student_id = #{studentId} AND task_id = #{taskId}")
    ExamRecord findByStudentAndTask(@Param("studentId") Long studentId, @Param("taskId") Long taskId);

    @Select("SELECT * FROM exam_record WHERE task_id = #{taskId} ORDER BY submit_time DESC")
    List<ExamRecord> findByTaskId(Long taskId);

    @Select("SELECT * FROM exam_record WHERE student_id = #{studentId} ORDER BY created_at DESC")
    List<ExamRecord> findByStudentId(Long studentId);

    @Insert("INSERT INTO exam_record (task_id, student_id, start_time, content, status) VALUES (#{taskId}, #{studentId}, #{startTime}, #{content}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamRecord record);

    @Update("UPDATE exam_record SET content = #{content}, updated_at = NOW() WHERE id = #{id}")
    int updateContent(ExamRecord record);

    @Update("UPDATE exam_record SET start_time = #{startTime}, status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int beginExam(ExamRecord record);

    @Update("UPDATE exam_record SET content = #{content}, submit_time = #{submitTime}, score = #{score}, status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int submit(ExamRecord record);

    @Update("UPDATE exam_record SET content = #{content}, submit_time = NOW(), status = 'AUTO_SUBMITTED', updated_at = NOW() WHERE id = #{id}")
    int autoSubmit(ExamRecord record);
}

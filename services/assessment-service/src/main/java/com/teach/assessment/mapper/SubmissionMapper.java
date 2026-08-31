package com.teach.assessment.mapper;

import org.apache.ibatis.annotations.*;
import com.teach.assessment.entity.Submission;
import java.util.List;

@Mapper
public interface SubmissionMapper {

    @Select("SELECT * FROM submission WHERE id = #{id}")
    Submission findById(Long id);

    @Select("SELECT * FROM submission WHERE task_id = #{taskId} ORDER BY submitted_at DESC")
    List<Submission> findByTaskId(Long taskId);

    @Select("SELECT * FROM submission WHERE student_id = #{studentId} AND task_id = #{taskId}")
    Submission findByStudentAndTask(@Param("studentId") Long studentId, @Param("taskId") Long taskId);

    @Select("SELECT * FROM submission WHERE student_id = #{studentId} ORDER BY submitted_at DESC")
    List<Submission> findByStudentId(Long studentId);

    @Select("SELECT s.* FROM submission s INNER JOIN task t ON s.task_id = t.id WHERE t.course_id = #{courseId} ORDER BY s.submitted_at DESC")
    List<Submission> findByCourseId(Long courseId);

    @Select("SELECT COUNT(*) FROM submission")
    int countAll();

    @Insert("INSERT INTO submission (task_id, student_id, content, file_path, status, judge_result) VALUES (#{taskId}, #{studentId}, #{content}, #{filePath}, #{status}, #{judgeResult})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Submission submission);

    @Update("UPDATE submission SET content=#{content}, file_path=#{filePath}, submitted_at=NOW() WHERE id=#{id}")
    int updateContent(Submission submission);

    @Update("UPDATE submission SET score=#{score}, status=#{status}, judge_result=#{judgeResult}, feedback=#{feedback} WHERE id=#{id}")
    int grade(Submission submission);
}

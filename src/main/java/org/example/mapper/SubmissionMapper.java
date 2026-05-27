package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.Submission;
import java.util.List;

@Mapper
public interface SubmissionMapper {

    @Select("SELECT s.*, u.name AS student_name, t.title AS task_title FROM submission s LEFT JOIN user u ON s.student_id = u.id LEFT JOIN task t ON s.task_id = t.id WHERE s.id = #{id}")
    Submission findById(Long id);

    @Select("SELECT s.*, u.name AS student_name, t.title AS task_title FROM submission s LEFT JOIN user u ON s.student_id = u.id LEFT JOIN task t ON s.task_id = t.id WHERE s.task_id = #{taskId} ORDER BY s.submitted_at DESC")
    List<Submission> findByTaskId(Long taskId);

    @Select("SELECT s.*, u.name AS student_name, t.title AS task_title FROM submission s LEFT JOIN user u ON s.student_id = u.id LEFT JOIN task t ON s.task_id = t.id WHERE s.student_id = #{studentId} AND s.task_id = #{taskId}")
    Submission findByStudentAndTask(@Param("studentId") Long studentId, @Param("taskId") Long taskId);

    @Select("SELECT s.*, t.title AS task_title, c.name AS course_name FROM submission s LEFT JOIN task t ON s.task_id = t.id LEFT JOIN course c ON t.course_id = c.id WHERE s.student_id = #{studentId} ORDER BY s.submitted_at DESC")
    List<Submission> findByStudentId(Long studentId);

    @Insert("INSERT INTO submission (task_id, student_id, content, file_path, status, judge_result) VALUES (#{taskId}, #{studentId}, #{content}, #{filePath}, #{status}, #{judgeResult})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Submission submission);

    @Update("UPDATE submission SET content=#{content}, file_path=#{filePath}, submitted_at=NOW() WHERE id=#{id}")
    int updateContent(Submission submission);

    @Update("UPDATE submission SET score=#{score}, status=#{status}, judge_result=#{judgeResult}, feedback=#{feedback} WHERE id=#{id}")
    int grade(Submission submission);
}

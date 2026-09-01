package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.Task;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Mapper
@ConditionalOnProperty(name="app.bff.enabled", havingValue="false", matchIfMissing=true)
public interface TaskMapper {

    @Select("SELECT t.*, c.name AS course_name FROM task t LEFT JOIN course c ON t.course_id = c.id WHERE t.id = #{id}")
    Task findById(Long id);

    @Select("SELECT t.*, c.name AS course_name FROM task t LEFT JOIN course c ON t.course_id = c.id WHERE t.course_id = #{courseId} AND t.status <> 'retracted' ORDER BY t.created_at DESC")
    List<Task> findByCourseId(Long courseId);

    @Select("SELECT t.*, c.name AS course_name FROM task t LEFT JOIN course c ON t.course_id = c.id INNER JOIN course_enrollment ce ON c.id = ce.course_id WHERE ce.student_id = #{studentId} AND t.status = 'published' ORDER BY t.end_time ASC")
    List<Task> findByStudentId(Long studentId);

    @Select("SELECT COUNT(*) FROM task")
    int countAll();

    @Insert("INSERT INTO task (title, description, course_id, type, max_score, time_limit_ms, memory_limit_mb, code_template, end_time, status) VALUES (#{title}, #{description}, #{courseId}, #{type}, #{maxScore}, #{timeLimitMs}, #{memoryLimitMb}, #{codeTemplate}, #{endTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Task task);

    @Update("UPDATE task SET title=#{title}, description=#{description}, type=#{type}, max_score=#{maxScore}, time_limit_ms=#{timeLimitMs}, memory_limit_mb=#{memoryLimitMb}, code_template=#{codeTemplate}, end_time=#{endTime} WHERE id=#{id}")
    int update(Task task);

    @Update("UPDATE task SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM task WHERE id=#{id}")
    int delete(Long id);
}

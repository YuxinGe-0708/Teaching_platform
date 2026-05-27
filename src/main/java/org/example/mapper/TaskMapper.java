package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.Task;
import java.util.List;

@Mapper
public interface TaskMapper {

    @Select("SELECT t.*, c.name AS course_name FROM task t LEFT JOIN course c ON t.course_id = c.id WHERE t.id = #{id}")
    Task findById(Long id);

    @Select("SELECT t.*, c.name AS course_name FROM task t LEFT JOIN course c ON t.course_id = c.id WHERE t.course_id = #{courseId} ORDER BY t.created_at DESC")
    List<Task> findByCourseId(Long courseId);

    @Select("SELECT t.*, c.name AS course_name FROM task t LEFT JOIN course c ON t.course_id = c.id INNER JOIN course_enrollment ce ON c.id = ce.course_id WHERE ce.student_id = #{studentId} AND t.status = 'published' ORDER BY t.end_time ASC")
    List<Task> findByStudentId(Long studentId);

    @Insert("INSERT INTO task (title, description, course_id, type, max_score, end_time, status) VALUES (#{title}, #{description}, #{courseId}, #{type}, #{maxScore}, #{endTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Task task);

    @Update("UPDATE task SET title=#{title}, description=#{description}, type=#{type}, max_score=#{maxScore}, end_time=#{endTime} WHERE id=#{id}")
    int update(Task task);

    @Update("UPDATE task SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM task WHERE id=#{id}")
    int delete(Long id);
}

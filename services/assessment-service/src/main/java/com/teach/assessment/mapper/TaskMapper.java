package com.teach.assessment.mapper;

import org.apache.ibatis.annotations.*;
import com.teach.assessment.entity.Task;
import java.util.List;

@Mapper
public interface TaskMapper {

    @Select("SELECT * FROM task WHERE id = #{id}")
    Task findById(Long id);

    @Select("SELECT * FROM task WHERE course_id = #{courseId} AND status <> 'retracted' ORDER BY created_at DESC")
    List<Task> findByCourseId(Long courseId);

    @Select("SELECT t.* FROM task t WHERE t.status = 'published' ORDER BY t.end_time ASC")
    List<Task> findByStudentId(Long studentId);

    @Select("SELECT * FROM task WHERE status = 'published' ORDER BY end_time ASC")
    List<Task> findPublished();

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

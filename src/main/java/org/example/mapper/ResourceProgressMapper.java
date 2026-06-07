package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.ResourceProgress;

import java.util.List;

@Mapper
public interface ResourceProgressMapper {

    @Select("SELECT * FROM resource_progress WHERE student_id = #{studentId} AND resource_id = #{resourceId}")
    ResourceProgress find(@Param("studentId") Long studentId, @Param("resourceId") Long resourceId);

    @Select("SELECT rp.* FROM resource_progress rp INNER JOIN resource r ON rp.resource_id = r.id WHERE rp.student_id = #{studentId} AND r.course_id = #{courseId}")
    List<ResourceProgress> findByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Select("SELECT rp.* FROM resource_progress rp INNER JOIN resource r ON rp.resource_id = r.id WHERE r.course_id = #{courseId}")
    List<ResourceProgress> findByCourseId(Long courseId);

    @Insert("INSERT INTO resource_progress (student_id, resource_id, progress, last_position, duration) VALUES (#{studentId}, #{resourceId}, #{progress}, #{lastPosition}, #{duration})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResourceProgress progress);

    @Update("UPDATE resource_progress SET progress=#{progress}, last_position=#{lastPosition}, duration=#{duration}, updated_at=NOW() WHERE student_id=#{studentId} AND resource_id=#{resourceId}")
    int update(ResourceProgress progress);
}

package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.TeachingResource;

import java.util.List;

@Mapper
public interface TeachingResourceMapper {

    @Select("SELECT r.*, c.name AS course_name FROM resource r LEFT JOIN course c ON r.course_id = c.id WHERE r.id = #{id}")
    TeachingResource findById(Long id);

    @Select("SELECT r.*, c.name AS course_name FROM resource r LEFT JOIN course c ON r.course_id = c.id WHERE r.course_id = #{courseId} ORDER BY r.created_at DESC")
    List<TeachingResource> findByCourseId(Long courseId);

    @Insert("INSERT INTO resource (course_id, title, file_path, type) VALUES (#{courseId}, #{title}, #{filePath}, #{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TeachingResource resource);

    @Delete("DELETE FROM resource WHERE id = #{id}")
    int deleteById(Long id);
}

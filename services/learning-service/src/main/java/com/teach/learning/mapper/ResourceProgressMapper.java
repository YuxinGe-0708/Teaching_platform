package com.teach.learning.mapper;
import com.teach.learning.entity.ResourceProgress;
import org.apache.ibatis.annotations.*;
@Mapper
public interface ResourceProgressMapper {
    @Select("SELECT * FROM resource_progress WHERE student_id = #{studentId} AND resource_id = #{resourceId}")
    ResourceProgress findByStudentAndResource(@Param("studentId") Long studentId, @Param("resourceId") Long resourceId);
    @Insert("INSERT INTO resource_progress (student_id, resource_id, progress, last_position, duration) VALUES (#{studentId}, #{resourceId}, #{progress}, #{lastPosition}, #{duration})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResourceProgress rp);
    @Update("UPDATE resource_progress SET progress=#{progress}, last_position=#{lastPosition}, duration=#{duration} WHERE student_id=#{studentId} AND resource_id=#{resourceId}")
    int update(ResourceProgress rp);
}

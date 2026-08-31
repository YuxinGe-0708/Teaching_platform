package com.teach.learning.mapper;
import com.teach.learning.entity.Resource;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface ResourceMapper {
    @Select("SELECT * FROM resource WHERE id = #{id}")
    Resource findById(Long id);
    @Select("SELECT * FROM resource WHERE course_id = #{courseId} ORDER BY created_at DESC")
    List<Resource> findByCourseId(Long courseId);
    @Insert("INSERT INTO resource (course_id, title, file_path, type, chapter, file_size, download_count) VALUES (#{courseId}, #{title}, #{filePath}, #{type}, #{chapter}, #{fileSize}, #{downloadCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Resource resource);
    @Update("UPDATE resource SET title=#{title}, file_path=#{filePath}, type=#{type}, chapter=#{chapter} WHERE id=#{id}")
    int update(Resource resource);
    @Update("UPDATE resource SET download_count = download_count + 1 WHERE id=#{id}")
    int incrementDownloadCount(Long id);
    @Delete("DELETE FROM resource WHERE id=#{id}")
    int deleteById(Long id);
}

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

    @Select("<script>"
            + "SELECT r.*, c.name AS course_name FROM resource r LEFT JOIN course c ON r.course_id = c.id WHERE r.course_id = #{courseId} "
            + "<if test='type != null and type != \"\"'>AND r.type = #{type} </if>"
            + "<if test='chapter != null and chapter != \"\"'>AND r.chapter = #{chapter} </if>"
            + "ORDER BY r.chapter ASC, r.created_at DESC"
            + "</script>")
    List<TeachingResource> searchByCourse(@Param("courseId") Long courseId, @Param("type") String type, @Param("chapter") String chapter);

    @Insert("INSERT INTO resource (course_id, title, file_path, type, chapter, file_size, download_count) VALUES (#{courseId}, #{title}, #{filePath}, #{type}, #{chapter}, #{fileSize}, #{downloadCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TeachingResource resource);

    @Update("UPDATE resource SET title=#{title}, chapter=#{chapter}, type=#{type} WHERE id=#{id}")
    int updateMeta(TeachingResource resource);

    @Update("UPDATE resource SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(Long id);

    @Delete("DELETE FROM resource WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT r.*, c.name AS course_name FROM resource r LEFT JOIN course c ON r.course_id = c.id ORDER BY r.created_at DESC LIMIT 200")
    List<TeachingResource> findRecent();
}

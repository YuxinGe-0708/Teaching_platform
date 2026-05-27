package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.CourseClass;
import java.util.List;

@Mapper
public interface CourseClassMapper {

    @Select("SELECT * FROM course_class WHERE course_id = #{courseId} ORDER BY created_at")
    List<CourseClass> findByCourseId(Long courseId);

    @Select("SELECT * FROM course_class WHERE invite_code = #{inviteCode}")
    CourseClass findByInviteCode(String inviteCode);

    @Select("SELECT * FROM course_class WHERE id = #{id}")
    CourseClass findById(Long id);

    @Insert("INSERT INTO course_class (course_id, name, invite_code, max_count, current_count) VALUES (#{courseId}, #{name}, #{inviteCode}, #{maxCount}, #{currentCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseClass cc);

    @Delete("DELETE FROM course_class WHERE id = #{id}")
    int delete(Long id);

    @Update("UPDATE course_class SET current_count = current_count + 1 WHERE id = #{id} AND current_count < max_count")
    int incrementCount(Long id);
}
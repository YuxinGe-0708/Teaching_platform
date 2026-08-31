package com.teach.learning.mapper;
import com.teach.learning.entity.CourseClass;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface CourseClassMapper {
    @Select("SELECT * FROM course_class WHERE id = #{id}")
    CourseClass findById(Long id);
    @Select("SELECT * FROM course_class WHERE course_id = #{courseId}")
    List<CourseClass> findByCourseId(Long courseId);
    @Select("SELECT * FROM course_class WHERE invite_code = #{inviteCode}")
    CourseClass findByInviteCode(String inviteCode);
    @Insert("INSERT INTO course_class (course_id, name, invite_code, max_count, current_count) VALUES (#{courseId}, #{name}, #{inviteCode}, #{maxCount}, #{currentCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseClass cc);
    @Update("UPDATE course_class SET name=#{name}, max_count=#{maxCount} WHERE id=#{id}")
    int update(CourseClass cc);
    @Update("UPDATE course_class SET current_count = current_count + 1 WHERE id=#{id}")
    int incrementCount(Long id);
    @Update("UPDATE course_class SET current_count = GREATEST(current_count - 1, 0) WHERE id=#{id}")
    int decrementCount(Long id);
    @Delete("DELETE FROM course_class WHERE id=#{id}")
    int deleteById(Long id);
}

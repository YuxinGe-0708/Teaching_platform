package com.teach.learning.mapper;
import com.teach.learning.entity.Course;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface CourseMapper {
    @Select("SELECT * FROM course WHERE id = #{id}")
    Course findById(Long id);
    @Select("SELECT * FROM course WHERE teacher_id = #{teacherId} ORDER BY created_at DESC")
    List<Course> findByTeacherId(Long teacherId);
    @Select("SELECT c.* FROM course c JOIN course_enrollment e ON c.id = e.course_id WHERE e.student_id = #{studentId} ORDER BY c.created_at DESC")
    List<Course> findByStudentId(Long studentId);
    @Select("SELECT * FROM course WHERE teacher_id = #{teacherId} AND status = #{status} ORDER BY created_at DESC")
    List<Course> findByTeacherIdAndStatus(@Param("teacherId") Long teacherId, @Param("status") String status);
    @Select("SELECT * FROM course WHERE status = 'active' ORDER BY created_at DESC")
    List<Course> findActive();
    @Select("SELECT * FROM course WHERE invite_code = #{inviteCode}")
    Course findByInviteCode(String inviteCode);
    @Insert("INSERT INTO course (name, code, description, credits, subject_category, hours, teacher_id, invite_code, allow_join, status) VALUES (#{name}, #{code}, #{description}, #{credits}, #{subjectCategory}, #{hours}, #{teacherId}, #{inviteCode}, #{allowJoin}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Course course);
    @Update("UPDATE course SET name=#{name}, code=#{code}, description=#{description}, credits=#{credits}, subject_category=#{subjectCategory}, hours=#{hours}, allow_join=#{allowJoin}, status=#{status} WHERE id=#{id}")
    int update(Course course);
    @Update("UPDATE course SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    @Delete("DELETE FROM course WHERE id=#{id}")
    int deleteById(Long id);
}

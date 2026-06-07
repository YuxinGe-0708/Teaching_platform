package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.Course;
import java.util.List;

@Mapper
public interface CourseMapper {

    @Select("SELECT c.*, u.name AS teacher_name FROM course c LEFT JOIN user u ON c.teacher_id = u.id WHERE c.id = #{id}")
    Course findById(Long id);

    @Select("SELECT c.*, u.name AS teacher_name, (SELECT COUNT(*) FROM course_enrollment ce WHERE ce.course_id = c.id) AS student_count FROM course c LEFT JOIN user u ON c.teacher_id = u.id WHERE c.teacher_id = #{teacherId} AND c.status <> 'deleted' ORDER BY c.created_at DESC")
    List<Course> findByTeacherId(Long teacherId);

    @Select("<script>"
            + "SELECT c.*, u.name AS teacher_name, (SELECT COUNT(*) FROM course_enrollment ce WHERE ce.course_id = c.id) AS student_count "
            + "FROM course c LEFT JOIN user u ON c.teacher_id = u.id WHERE c.teacher_id = #{teacherId} AND c.status != 'deleted' "
            + "<if test='keyword != null and keyword != \"\"'>AND (c.name LIKE CONCAT('%', #{keyword}, '%') OR c.code LIKE CONCAT('%', #{keyword}, '%')) </if>"
            + "ORDER BY "
            + "<choose>"
            + "<when test='sort == \"students\"'>student_count DESC</when>"
            + "<when test='sort == \"oldest\"'>c.created_at ASC</when>"
            + "<otherwise>c.created_at DESC</otherwise>"
            + "</choose>"
            + "</script>")
    List<Course> searchTeacherCourses(@Param("teacherId") Long teacherId, @Param("keyword") String keyword, @Param("sort") String sort);

    @Select("SELECT c.*, u.name AS teacher_name, (SELECT COUNT(*) FROM course_enrollment ce WHERE ce.course_id = c.id) AS student_count FROM course c LEFT JOIN user u ON c.teacher_id = u.id WHERE c.status = 'active' AND c.allow_join = TRUE ORDER BY c.created_at DESC")
    List<Course> findAllActive();

    @Select("SELECT c.*, u.name AS teacher_name, (SELECT COUNT(*) FROM course_enrollment ce WHERE ce.course_id = c.id) AS student_count FROM course c LEFT JOIN user u ON c.teacher_id = u.id INNER JOIN course_enrollment ce ON c.id = ce.course_id WHERE ce.student_id = #{studentId} AND c.status = 'active' ORDER BY ce.enrolled_at DESC")
    List<Course> findByStudentId(Long studentId);

    @Select("SELECT c.*, u.name AS teacher_name FROM course c LEFT JOIN user u ON c.teacher_id = u.id WHERE c.invite_code = #{inviteCode} AND c.status = 'active'")
    Course findByInviteCode(String inviteCode);

    @Select("SELECT COUNT(*) FROM course")
    int countAll();

    @Insert("INSERT INTO course (name, code, description, credits, subject_category, hours, teacher_id, invite_code, cover_url, allow_join, status) VALUES (#{name}, #{code}, #{description}, #{credits}, #{subjectCategory}, #{hours}, #{teacherId}, #{inviteCode}, #{coverUrl}, #{allowJoin}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Course course);

    @Update("UPDATE course SET name=#{name}, code=#{code}, description=#{description}, credits=#{credits}, subject_category=#{subjectCategory}, hours=#{hours}, invite_code=#{inviteCode}, cover_url=#{coverUrl}, allow_join=#{allowJoin} WHERE id=#{id}")
    int update(Course course);

    @Update("UPDATE course SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM course WHERE id=#{id}")
    int delete(Long id);
}

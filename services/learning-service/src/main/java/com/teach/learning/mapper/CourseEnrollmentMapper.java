package com.teach.learning.mapper;
import com.teach.learning.entity.CourseEnrollment;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface CourseEnrollmentMapper {
    @Select("SELECT * FROM course_enrollment WHERE id = #{id}")
    CourseEnrollment findById(Long id);
    @Select("SELECT * FROM course_enrollment WHERE student_id = #{studentId}")
    List<CourseEnrollment> findByStudentId(Long studentId);
    @Select("SELECT * FROM course_enrollment WHERE course_id = #{courseId}")
    List<CourseEnrollment> findByCourseId(Long courseId);
    @Select("SELECT * FROM course_enrollment WHERE student_id = #{studentId} AND course_id = #{courseId}")
    CourseEnrollment findByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    @Select("SELECT COUNT(*) FROM course_enrollment WHERE course_id = #{courseId}")
    int countByCourseId(Long courseId);
    @Insert("INSERT INTO course_enrollment (student_id, course_id, class_id) VALUES (#{studentId}, #{courseId}, #{classId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseEnrollment enrollment);
    @Update("UPDATE course_enrollment SET class_id=#{classId} WHERE id=#{id}")
    int updateClass(@Param("id") Long id, @Param("classId") Long classId);
    @Delete("DELETE FROM course_enrollment WHERE student_id = #{studentId} AND course_id = #{courseId}")
    int deleteByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}

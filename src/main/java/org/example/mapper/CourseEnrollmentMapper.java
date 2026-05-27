package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.CourseEnrollment;

@Mapper
public interface CourseEnrollmentMapper {

    @Select("SELECT * FROM course_enrollment WHERE student_id = #{studentId} AND course_id = #{courseId}")
    CourseEnrollment findByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Insert("INSERT INTO course_enrollment (student_id, course_id) VALUES (#{studentId}, #{courseId})")
    int insert(CourseEnrollment enrollment);

    @Delete("DELETE FROM course_enrollment WHERE student_id = #{studentId} AND course_id = #{courseId}")
    int delete(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Select("SELECT COUNT(*) FROM course_enrollment WHERE course_id = #{courseId}")
    int countByCourseId(Long courseId);
}

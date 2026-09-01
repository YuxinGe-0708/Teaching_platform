package org.example.mapper;
import org.example.entity.CourseEnrollment;
import java.util.List;
public interface CourseEnrollmentMapper {
    CourseEnrollment findByStudentAndCourse(Long studentId,Long courseId); int insert(CourseEnrollment enrollment);
    List<CourseEnrollment> findByStudentId(Long studentId); List<CourseEnrollment> findByCourseId(Long courseId);
    int delete(Long studentId,Long courseId); int countByCourseId(Long courseId);
}

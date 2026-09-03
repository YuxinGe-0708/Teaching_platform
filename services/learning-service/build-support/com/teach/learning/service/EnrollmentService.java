package com.teach.learning.service;
import com.teach.learning.entity.Course;
import com.teach.learning.entity.CourseClass;
import com.teach.learning.entity.CourseEnrollment;
import com.teach.learning.mapper.CourseEnrollmentMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EnrollmentService {
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseService courseService;
    private final CourseClassService classService;
    public EnrollmentService(CourseEnrollmentMapper enrollmentMapper, CourseService courseService, CourseClassService classService) {
        this.enrollmentMapper = enrollmentMapper; this.courseService = courseService; this.classService = classService;
    }

    public CourseEnrollment enroll(Long studentId, Long courseId, Long classId) {
        Course course = courseService.findById(courseId);
        if (course == null || !"active".equals(course.getStatus())) return null;
        if (!Boolean.TRUE.equals(course.getAllowJoin())) return null;
        CourseEnrollment existing = enrollmentMapper.findByStudentAndCourse(studentId, courseId);
        if (existing != null) return existing;
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentId(studentId); enrollment.setCourseId(courseId);
        enrollment.setClassId(classId != null ? classId : findDefaultClassId(courseId));
        enrollmentMapper.insert(enrollment);
        if (enrollment.getClassId() != null) classService.incrementCount(enrollment.getClassId());
        return enrollment;
    }

    public boolean unenroll(Long studentId, Long courseId) {
        CourseEnrollment enrollment = enrollmentMapper.findByStudentAndCourse(studentId, courseId);
        if (enrollment == null) return false;
        if (enrollment.getClassId() != null) classService.decrementCount(enrollment.getClassId());
        return enrollmentMapper.deleteByStudentAndCourse(studentId, courseId) > 0;
    }

    public boolean removeFromClass(Long classId, Long studentId) {
        CourseEnrollment enrollment = enrollmentMapper.findByClassAndStudent(classId, studentId);
        if (enrollment == null) return false;
        if (enrollment.getClassId() != null) classService.decrementCount(enrollment.getClassId());
        return enrollmentMapper.deleteByStudentAndCourse(studentId, enrollment.getCourseId()) > 0;
    }

    public List<CourseEnrollment> getStudentEnrollments(Long studentId) { return enrollmentMapper.findByStudentId(studentId); }
    public List<CourseEnrollment> getCourseEnrollments(Long courseId) { return enrollmentMapper.findByCourseId(courseId); }
    public boolean isEnrolled(Long studentId, Long courseId) { return enrollmentMapper.findByStudentAndCourse(studentId, courseId) != null; }
    public int countByCourseId(Long courseId) { return enrollmentMapper.countByCourseId(courseId); }

    private Long findDefaultClassId(Long courseId) {
        List<CourseClass> classes = classService.findByCourseId(courseId);
        return classes.isEmpty() ? null : classes.get(0).getId();
    }
}

package org.example.mapper;
import org.example.entity.Course;
import java.util.List;
/** BFF port implemented by RemoteCourseMapper; it never executes SQL. */
public interface CourseMapper {
    Course findById(Long id); List<Course> findByTeacherId(Long teacherId);
    List<Course> searchTeacherCourses(Long teacherId,String keyword,String sort); List<Course> findAllActive();
    List<Course> findByStudentId(Long studentId); Course findByInviteCode(String inviteCode);
    int countAll(); int insert(Course course); int update(Course course); int updateStatus(Long id,String status); int delete(Long id);
}

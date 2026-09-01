package org.example.mapper;
import org.example.entity.CourseClass;
import java.util.List;
public interface CourseClassMapper {
    List<CourseClass> findByCourseId(Long courseId); CourseClass findByInviteCode(String inviteCode); CourseClass findById(Long id);
    int insert(CourseClass value); int delete(Long id); int deleteByCourse(Long courseId,Long classId);
    int update(CourseClass value); int incrementCount(Long id); int decrementCount(Long id);
}

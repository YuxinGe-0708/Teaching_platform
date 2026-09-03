package org.example.mapper;
import org.example.entity.ResourceProgress;
import java.util.List;
public interface ResourceProgressMapper {
    ResourceProgress find(Long studentId,Long resourceId); List<ResourceProgress> findByStudentAndCourse(Long studentId,Long courseId);
    List<ResourceProgress> findByCourseId(Long courseId); int insert(ResourceProgress value); int update(ResourceProgress value);
}

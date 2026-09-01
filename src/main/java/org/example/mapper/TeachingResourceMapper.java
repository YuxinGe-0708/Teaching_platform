package org.example.mapper;
import org.example.entity.TeachingResource;
import java.util.List;
public interface TeachingResourceMapper {
    TeachingResource findById(Long id); List<TeachingResource> findByCourseId(Long courseId);
    List<TeachingResource> searchByCourse(Long courseId,String type,String chapter); int insert(TeachingResource value);
    int updateMeta(TeachingResource value); int incrementDownloadCount(Long id); int deleteById(Long id); List<TeachingResource> findRecent();
}

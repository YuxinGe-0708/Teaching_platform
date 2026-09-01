package com.teach.learning.service;
import com.teach.learning.entity.ResourceProgress;
import com.teach.learning.mapper.ResourceProgressMapper;
import org.springframework.stereotype.Service;

@Service
public class ResourceProgressService {
    private final ResourceProgressMapper progressMapper;
    public ResourceProgressService(ResourceProgressMapper progressMapper) { this.progressMapper = progressMapper; }

    public ResourceProgress findByStudentAndResource(Long studentId, Long resourceId) {
        return progressMapper.findByStudentAndResource(studentId, resourceId);
    }

    public ResourceProgress save(Long studentId, Long resourceId, Double progress, Double lastPosition, Double duration) {
        ResourceProgress rp = progressMapper.findByStudentAndResource(studentId, resourceId);
        if (rp == null) {
            rp = new ResourceProgress();
            rp.setStudentId(studentId); rp.setResourceId(resourceId);
            rp.setProgress(progress); rp.setLastPosition(lastPosition); rp.setDuration(duration);
            progressMapper.insert(rp);
        } else {
            rp.setProgress(progress); rp.setLastPosition(lastPosition); rp.setDuration(duration);
            progressMapper.update(rp);
        }
        return rp;
    }
}

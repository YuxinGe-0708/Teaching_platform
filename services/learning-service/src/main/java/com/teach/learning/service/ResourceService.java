package com.teach.learning.service;
import com.teach.learning.entity.Resource;
import com.teach.learning.mapper.ResourceMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResourceService {
    private final ResourceMapper resourceMapper;
    public ResourceService(ResourceMapper resourceMapper) { this.resourceMapper = resourceMapper; }

    public Resource create(Long courseId, String title, String filePath, String type, String chapter, Long fileSize) {
        Resource r = new Resource();
        r.setCourseId(courseId); r.setTitle(title); r.setFilePath(filePath != null ? filePath : "");
        r.setType(type != null ? type : "other"); r.setChapter(chapter != null ? chapter : "默认章节");
        r.setFileSize(fileSize != null ? fileSize : 0); r.setDownloadCount(0);
        resourceMapper.insert(r); return r;
    }

    public Resource findById(Long id) { return resourceMapper.findById(id); }
    public List<Resource> findByCourseId(Long courseId) { return resourceMapper.findByCourseId(courseId); }
    public boolean update(Long id, String title, String filePath, String type, String chapter) {
        Resource r = resourceMapper.findById(id); if (r == null) return false;
        r.setTitle(title); r.setFilePath(filePath); r.setType(type); r.setChapter(chapter);
        return resourceMapper.update(r) > 0;
    }
    public boolean delete(Long id) { return resourceMapper.deleteById(id) > 0; }
    public void incrementDownloadCount(Long id) { resourceMapper.incrementDownloadCount(id); }
}

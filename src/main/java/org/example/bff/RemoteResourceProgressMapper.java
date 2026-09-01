package org.example.bff;
import org.example.entity.ResourceProgress; import org.example.mapper.ResourceProgressMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteResourceProgressMapper implements ResourceProgressMapper {
 private final MicroserviceClient c; public RemoteResourceProgressMapper(MicroserviceClient c){this.c=c;}
 public ResourceProgress find(Long s,Long r){return c.get(c.uri(c.learning("/internal/bff/progress")).queryParam("studentId",s).queryParam("resourceId",r).toUriString(),ResourceProgress.class);}
 public List<ResourceProgress> findByStudentAndCourse(Long s,Long co){return c.getList(c.uri(c.learning("/internal/bff/progress/student-course")).queryParam("studentId",s).queryParam("courseId",co).toUriString(),ResourceProgress.class);}
 public List<ResourceProgress> findByCourseId(Long co){return c.getList(c.learning("/internal/bff/progress/course/"+co),ResourceProgress.class);}
 public int insert(ResourceProgress v){ResourceProgress saved=c.post(c.learning("/internal/bff/progress"),v,ResourceProgress.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int update(ResourceProgress v){Integer n=c.put(c.learning("/internal/bff/progress"),v,Integer.class);return n==null?0:n;}
}

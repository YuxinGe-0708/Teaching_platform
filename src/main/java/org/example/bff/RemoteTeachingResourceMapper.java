package org.example.bff;
import org.example.entity.Course; import org.example.entity.TeachingResource; import org.example.mapper.TeachingResourceMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteTeachingResourceMapper implements TeachingResourceMapper {
 private final MicroserviceClient c; public RemoteTeachingResourceMapper(MicroserviceClient c){this.c=c;}
 public TeachingResource findById(Long id){return enrich(c.get(c.learning("/internal/bff/resources/"+id),TeachingResource.class));}
 public List<TeachingResource> findByCourseId(Long id){List<TeachingResource> v=c.getList(c.uri(c.learning("/internal/bff/resources")).queryParam("courseId",id).toUriString(),TeachingResource.class);v.forEach(this::enrich);return v;}
 public List<TeachingResource> searchByCourse(Long id,String type,String chapter){org.springframework.web.util.UriComponentsBuilder u=c.uri(c.learning("/internal/bff/resources")).queryParam("courseId",id);if(type!=null&&!type.isEmpty())u.queryParam("type",type);if(chapter!=null&&!chapter.isEmpty())u.queryParam("chapter",chapter);List<TeachingResource> v=c.getList(u.toUriString(),TeachingResource.class);v.forEach(this::enrich);return v;}
 public int insert(TeachingResource v){TeachingResource saved=c.post(c.learning("/internal/bff/resources"),v,TeachingResource.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int updateMeta(TeachingResource v){return n(c.put(c.learning("/internal/bff/resources/"+v.getId()),v,Integer.class));}
 public int incrementDownloadCount(Long id){return n(c.put(c.learning("/internal/bff/resources/"+id+"/download"),null,Integer.class));}
 public int deleteById(Long id){return n(c.delete(c.learning("/internal/bff/resources/"+id),Integer.class));}
 public List<TeachingResource> findRecent(){List<TeachingResource> v=c.getList(c.learning("/internal/bff/resources/recent"),TeachingResource.class);v.forEach(this::enrich);return v;}
 private TeachingResource enrich(TeachingResource r){if(r!=null&&r.getCourseId()!=null){Course x=c.get(c.learning("/internal/bff/courses/"+r.getCourseId()),Course.class);if(x!=null)r.setCourseName(x.getName());}return r;} private int n(Integer v){return v==null?0:v;}
}

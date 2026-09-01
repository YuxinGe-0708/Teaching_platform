package org.example.bff;
import org.example.entity.*; import org.example.mapper.TaskMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteTaskMapper implements TaskMapper {
 private final MicroserviceClient c; public RemoteTaskMapper(MicroserviceClient c){this.c=c;}
 public Task findById(Long id){return enrich(c.get(c.assessment("/internal/bff/tasks/"+id),Task.class));}
 public List<Task> findByCourseId(Long id){List<Task> v=c.getList(c.uri(c.assessment("/internal/bff/tasks")).queryParam("courseId",id).toUriString(),Task.class);v.forEach(this::enrich);return v;}
 public List<Task> findByStudentId(Long id){List<Task> v=c.getList(c.uri(c.assessment("/internal/bff/tasks")).queryParam("studentId",id).toUriString(),Task.class);v.forEach(this::enrich);return v;}
 public int countAll(){return n(c.get(c.assessment("/internal/bff/tasks/count"),Integer.class));}
 public int insert(Task v){Task saved=c.post(c.assessment("/internal/bff/tasks"),v,Task.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int update(Task v){return n(c.put(c.assessment("/internal/bff/tasks/"+v.getId()),v,Integer.class));}
 public int updateStatus(Long id,String status){return n(c.put(c.uri(c.assessment("/internal/bff/tasks/"+id+"/status")).queryParam("status",status).toUriString(),null,Integer.class));}
 public int delete(Long id){return n(c.delete(c.assessment("/internal/bff/tasks/"+id),Integer.class));}
 private Task enrich(Task v){if(v!=null&&v.getCourseId()!=null){Course co=c.get(c.learning("/internal/bff/courses/"+v.getCourseId()),Course.class);if(co!=null)v.setCourseName(co.getName());}return v;} private int n(Integer v){return v==null?0:v;}
}

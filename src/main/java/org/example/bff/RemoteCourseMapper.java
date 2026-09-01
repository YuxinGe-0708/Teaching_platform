package org.example.bff;
import org.example.entity.Course; import org.example.mapper.CourseMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary;
import java.util.*; import java.util.stream.Collectors;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteCourseMapper implements CourseMapper {
 private final MicroserviceClient c; public RemoteCourseMapper(MicroserviceClient c){this.c=c;}
 public Course findById(Long id){return c.get(c.learning("/internal/bff/courses/"+id),Course.class);}
 public List<Course> findByTeacherId(Long id){return c.getList(c.uri(c.learning("/internal/bff/courses")).queryParam("teacherId",id).toUriString(),Course.class);}
 public List<Course> searchTeacherCourses(Long id,String keyword,String sort){List<Course> values=findByTeacherId(id);String k=keyword==null?"":keyword.toLowerCase();values=values.stream().filter(x->k.isEmpty()||text(x.getName()).contains(k)||text(x.getCode()).contains(k)).collect(Collectors.toList());if("oldest".equals(sort))Collections.reverse(values);return values;}
 public List<Course> findAllActive(){return c.getList(c.uri(c.learning("/internal/bff/courses")).queryParam("active",true).toUriString(),Course.class);}
 public List<Course> findByStudentId(Long id){return c.getList(c.uri(c.learning("/internal/bff/courses")).queryParam("studentId",id).toUriString(),Course.class);}
 public Course findByInviteCode(String code){return c.get(c.uri(c.learning("/internal/bff/courses/by-invite")).queryParam("inviteCode",code).toUriString(),Course.class);}
 public int countAll(){return n(c.get(c.learning("/internal/bff/courses/count"),Integer.class));}
 public int insert(Course v){Course saved=c.post(c.learning("/internal/bff/courses"),v,Course.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int update(Course v){return n(c.put(c.learning("/internal/bff/courses/"+v.getId()),v,Integer.class));}
 public int updateStatus(Long id,String status){return n(c.put(c.uri(c.learning("/internal/bff/courses/"+id+"/status")).queryParam("status",status).toUriString(),null,Integer.class));}
 public int delete(Long id){return n(c.delete(c.learning("/internal/bff/courses/"+id),Integer.class));}
 private int n(Integer v){return v==null?0:v;} private String text(String v){return v==null?"":v.toLowerCase();}
}

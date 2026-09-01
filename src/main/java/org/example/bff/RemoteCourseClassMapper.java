package org.example.bff;
import org.example.entity.CourseClass; import org.example.mapper.CourseClassMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteCourseClassMapper implements CourseClassMapper {
 private final MicroserviceClient c; public RemoteCourseClassMapper(MicroserviceClient c){this.c=c;}
 public List<CourseClass> findByCourseId(Long id){return c.getList(c.uri(c.learning("/internal/bff/classes")).queryParam("courseId",id).toUriString(),CourseClass.class);}
 public CourseClass findByInviteCode(String code){return c.get(c.uri(c.learning("/internal/bff/classes/by-invite")).queryParam("inviteCode",code).toUriString(),CourseClass.class);}
 public CourseClass findById(Long id){return c.get(c.learning("/internal/bff/classes/"+id),CourseClass.class);}
 public int insert(CourseClass v){CourseClass saved=c.post(c.learning("/internal/bff/classes"),v,CourseClass.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int delete(Long id){return n(c.delete(c.learning("/internal/bff/classes/"+id),Integer.class));}
 public int deleteByCourse(Long courseId,Long classId){CourseClass v=findById(classId);return v!=null&&courseId.equals(v.getCourseId())?delete(classId):0;}
 public int update(CourseClass v){return n(c.put(c.learning("/internal/bff/classes/"+v.getId()),v,Integer.class));}
 public int incrementCount(Long id){return n(c.put(c.learning("/internal/bff/classes/"+id+"/increment"),null,Integer.class));}
 public int decrementCount(Long id){return n(c.put(c.learning("/internal/bff/classes/"+id+"/decrement"),null,Integer.class));}
 private int n(Integer v){return v==null?0:v;}
}

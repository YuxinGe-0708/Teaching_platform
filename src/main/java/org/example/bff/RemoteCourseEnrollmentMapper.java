package org.example.bff;
import org.example.entity.CourseEnrollment; import org.example.mapper.CourseEnrollmentMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteCourseEnrollmentMapper implements CourseEnrollmentMapper {
 private final MicroserviceClient c; public RemoteCourseEnrollmentMapper(MicroserviceClient c){this.c=c;}
 public CourseEnrollment findByStudentAndCourse(Long s,Long co){return c.get(c.uri(c.learning("/internal/bff/enrollments/check")).queryParam("studentId",s).queryParam("courseId",co).toUriString(),CourseEnrollment.class);}
 public int insert(CourseEnrollment v){CourseEnrollment saved=c.post(c.learning("/internal/bff/enrollments"),v,CourseEnrollment.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public List<CourseEnrollment> findByStudentId(Long id){return c.getList(c.learning("/internal/bff/enrollments/student/"+id),CourseEnrollment.class);}
 public List<CourseEnrollment> findByCourseId(Long id){return c.getList(c.learning("/internal/bff/enrollments/course/"+id),CourseEnrollment.class);}
 public int delete(Long s,Long co){return n(c.delete(c.uri(c.learning("/internal/bff/enrollments")).queryParam("studentId",s).queryParam("courseId",co).toUriString(),Integer.class));}
 public int countByCourseId(Long id){return n(c.get(c.learning("/internal/bff/enrollments/count/"+id),Integer.class));}
 private int n(Integer v){return v==null?0:v;}
}

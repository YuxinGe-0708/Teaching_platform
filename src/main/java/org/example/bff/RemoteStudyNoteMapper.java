package org.example.bff;
import org.example.entity.StudyNote; import org.example.mapper.StudyNoteMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteStudyNoteMapper implements StudyNoteMapper {
 private final MicroserviceClient c; public RemoteStudyNoteMapper(MicroserviceClient c){this.c=c;}
 public List<StudyNote> findByStudentId(Long id){return c.getList(c.learning("/internal/bff/notes/student/"+id),StudyNote.class);}
 public List<StudyNote> findByStudentAndCourse(Long s,Long co){return c.getList(c.uri(c.learning("/internal/bff/notes/student-course")).queryParam("studentId",s).queryParam("courseId",co).toUriString(),StudyNote.class);}
 public StudyNote findById(Long id){return c.get(c.learning("/internal/bff/notes/"+id),StudyNote.class);}
 public int insert(StudyNote v){StudyNote saved=c.post(c.learning("/internal/bff/notes"),v,StudyNote.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int update(StudyNote v){return n(c.put(c.learning("/internal/bff/notes/"+v.getId()),v,Integer.class));}
 public int updateMindMap(StudyNote v){return n(c.put(c.learning("/internal/bff/notes/"+v.getId()+"/mind-map"),v,Integer.class));}
 public int deleteByStudent(Long id,Long studentId){StudyNote v=findById(id);return v!=null&&studentId.equals(v.getStudentId())?n(c.delete(c.learning("/internal/bff/notes/"+id),Integer.class)):0;}
 private int n(Integer v){return v==null?0:v;}
}

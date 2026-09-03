package org.example.bff;
import org.example.entity.*; import org.example.mapper.SubmissionMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteSubmissionMapper implements SubmissionMapper {
 private final MicroserviceClient c; public RemoteSubmissionMapper(MicroserviceClient c){this.c=c;}
 public Submission findById(Long id){return enrich(c.get(c.assessment("/internal/bff/submissions/"+id),Submission.class));}
 public List<Submission> findByTaskId(Long id){List<Submission> v=c.getList(c.assessment("/internal/bff/submissions/task/"+id),Submission.class);v.forEach(this::enrich);return v;}
 public Submission findByStudentAndTask(Long s,Long t){return enrich(c.get(c.uri(c.assessment("/internal/bff/submissions/student-task")).queryParam("studentId",s).queryParam("taskId",t).toUriString(),Submission.class));}
 public List<Submission> findByStudentId(Long id){List<Submission> v=c.getList(c.assessment("/internal/bff/submissions/student/"+id),Submission.class);v.forEach(this::enrich);return v;}
 public List<Submission> findByCourseId(Long id){List<Submission> v=c.getList(c.assessment("/internal/bff/submissions/course/"+id),Submission.class);v.forEach(this::enrich);return v;}
 public int countAll(){return n(c.get(c.assessment("/internal/bff/submissions/count"),Integer.class));}
 public int insert(Submission v){Submission saved=c.post(c.assessment("/internal/bff/submissions"),v,Submission.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int updateContent(Submission v){return n(c.put(c.assessment("/internal/bff/submissions/"+v.getId()),v,Integer.class));}
 public int grade(Submission v){return n(c.put(c.assessment("/internal/bff/submissions/"+v.getId()+"/grade"),v,Integer.class));}
 private Submission enrich(Submission v){if(v!=null){if(v.getStudentId()!=null){User u=c.get(c.user("/internal/bff/users/"+v.getStudentId()),User.class);if(u!=null)v.setStudentName(u.getName()==null?u.getUsername():u.getName());}if(v.getTaskId()!=null){Task t=c.get(c.assessment("/internal/bff/tasks/"+v.getTaskId()),Task.class);if(t!=null)v.setTaskTitle(t.getTitle());}}return v;} private int n(Integer v){return v==null?0:v;}
}

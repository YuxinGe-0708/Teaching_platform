package org.example.bff;
import org.example.entity.ExamRecord; import org.example.mapper.ExamRecordMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteExamRecordMapper implements ExamRecordMapper {
 private final MicroserviceClient c; public RemoteExamRecordMapper(MicroserviceClient c){this.c=c;}
 public ExamRecord findById(Long id){return c.get(c.assessment("/internal/bff/exams/"+id),ExamRecord.class);}
 public ExamRecord findByStudentAndTask(Long s,Long t){return c.get(c.uri(c.assessment("/internal/bff/exams/student-task")).queryParam("studentId",s).queryParam("taskId",t).toUriString(),ExamRecord.class);}
 public List<ExamRecord> findByTaskId(Long id){return c.getList(c.assessment("/internal/bff/exams/task/"+id),ExamRecord.class);}
 public List<ExamRecord> findByStudentId(Long id){return c.getList(c.assessment("/internal/bff/exams/student/"+id),ExamRecord.class);}
 public int insert(ExamRecord v){ExamRecord saved=c.post(c.assessment("/internal/bff/exams"),v,ExamRecord.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int updateContent(ExamRecord v){return n(c.put(c.assessment("/internal/bff/exams/"+v.getId()+"/content"),v,Integer.class));}
 public int beginExam(ExamRecord v){return n(c.put(c.assessment("/internal/bff/exams/"+v.getId()+"/begin"),v,Integer.class));}
 public int submit(ExamRecord v){return n(c.put(c.assessment("/internal/bff/exams/"+v.getId()+"/submit"),v,Integer.class));}
 public int autoSubmit(ExamRecord v){return n(c.put(c.assessment("/internal/bff/exams/"+v.getId()+"/auto-submit"),v,Integer.class));}
 private int n(Integer v){return v==null?0:v;}
}

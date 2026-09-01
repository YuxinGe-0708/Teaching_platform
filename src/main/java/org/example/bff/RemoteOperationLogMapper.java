package org.example.bff;
import org.example.entity.OperationLog; import org.example.mapper.OperationLogMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteOperationLogMapper implements OperationLogMapper {
 private final MicroserviceClient c; public RemoteOperationLogMapper(MicroserviceClient c){this.c=c;}
 public int insert(OperationLog v){OperationLog saved=c.post(c.user("/internal/bff/logs"),v,OperationLog.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public List<OperationLog> findByUserId(Long id){return c.getList(c.user("/internal/bff/logs/user/"+id),OperationLog.class);}
 public List<OperationLog> findRecent(){return c.getList(c.user("/internal/bff/logs/recent"),OperationLog.class);}
}

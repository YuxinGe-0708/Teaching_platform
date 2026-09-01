package org.example.bff;
import org.example.entity.Notification; import org.example.mapper.NotificationMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary;
import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteNotificationMapper implements NotificationMapper {
 private final MicroserviceClient c; public RemoteNotificationMapper(MicroserviceClient c){this.c=c;}
 public List<Notification> findByUserId(Long id){return c.getList(c.user("/internal/bff/notifications/user/"+id),Notification.class);}
 public Notification findById(Long id){return c.get(c.user("/internal/bff/notifications/"+id),Notification.class);}
 public List<Notification> findRecent(){return c.getList(c.user("/internal/bff/notifications/recent"),Notification.class);}
 public int countUnread(Long id){return n(c.get(c.user("/internal/bff/notifications/unread-count/"+id),Integer.class));}
 public int insert(Notification v){Notification saved=c.post(c.user("/internal/bff/notifications"),v,Notification.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public int markAsRead(Long id){return n(c.put(c.user("/internal/bff/notifications/"+id+"/read"),null,Integer.class));}
 public int markAsReadForUser(Long id,Long userId){return n(c.put(c.uri(c.user("/internal/bff/notifications/"+id+"/read")).queryParam("userId",userId).toUriString(),null,Integer.class));}
 public int markAllAsRead(Long id){return n(c.put(c.user("/internal/bff/notifications/read-all/"+id),null,Integer.class));}
 public int deleteById(Long id){return n(c.delete(c.user("/internal/bff/notifications/"+id),Integer.class));}
 private int n(Integer v){return v==null?0:v;}
}

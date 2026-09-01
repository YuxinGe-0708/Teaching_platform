package org.example.mapper;
import org.example.entity.Notification;
import java.util.List;
public interface NotificationMapper {
    List<Notification> findByUserId(Long userId); Notification findById(Long id); List<Notification> findRecent(); int countUnread(Long userId);
    int insert(Notification value); int markAsRead(Long id); int markAsReadForUser(Long id,Long userId);
    int markAllAsRead(Long userId); int deleteById(Long id);
}

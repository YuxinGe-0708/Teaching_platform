package com.teach.user.service;

import com.teach.user.entity.Notification;
import com.teach.user.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /** 创建单条站内通知（供其它服务经 /internal 调用）。 */
    public Notification create(Long userId, String title, String content, String type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type == null || type.trim().isEmpty() ? "system" : type);
        notificationMapper.insert(n);
        return n;
    }

    public List<Notification> findByUserId(Long userId) {
        return notificationMapper.findByUserId(userId);
    }

    public int countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    public boolean markAsRead(Long notificationId, Long userId) {
        return notificationMapper.markAsReadForUser(notificationId, userId) > 0;
    }

    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }
}

package com.teach.user.service;

import com.teach.user.entity.Notification;
import com.teach.user.mapper.NotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationMapper notificationMapper;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  @DisplayName("UNIT-TC-NOTIF-01: 创建通知（指定类型）")
  void testCreate_WithCustomType() {
    when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

    Notification n = notificationService.create(1L, "作业提醒", "请在周五前提交", "homework");

    assertNotNull(n);
    assertEquals(1L, n.getUserId());
    assertEquals("作业提醒", n.getTitle());
    assertEquals("homework", n.getType());
    verify(notificationMapper).insert(any(Notification.class));
  }

  @Test
  @DisplayName("UNIT-TC-NOTIF-02: 创建通知（类型为空时默认 system）")
  void testCreate_WithDefaultType() {
    when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

    Notification n = notificationService.create(1L, "系统通知", "维护公告", "  ");

    assertEquals("system", n.getType());
  }

  @Test
  @DisplayName("UNIT-TC-NOTIF-03: 获取用户通知与未读数量统计")
  void testFindByUserIdAndCount() {
    Notification n = new Notification();
    n.setId(10L);
    n.setUserId(1L);
    n.setIsRead(false);

    when(notificationMapper.findByUserId(1L)).thenReturn(Collections.singletonList(n));
    when(notificationMapper.countUnread(1L)).thenReturn(1);

    List<Notification> list = notificationService.findByUserId(1L);
    int unreadCount = notificationService.countUnread(1L);

    assertEquals(1, list.size());
    assertEquals(1, unreadCount);
    assertFalse(list.get(0).getIsRead());
  }

  @Test
  @DisplayName("UNIT-TC-NOTIF-04: 标记单条通知已读与一键全部已读")
  void testMarkAsRead() {
    when(notificationMapper.markAsReadForUser(10L, 1L)).thenReturn(1);

    boolean result = notificationService.markAsRead(10L, 1L);

    assertTrue(result);
    verify(notificationMapper).markAsReadForUser(10L, 1L);

    notificationService.markAllAsRead(1L);
    verify(notificationMapper).markAllAsRead(1L);
  }
}
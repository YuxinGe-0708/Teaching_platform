package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.Notification;
import java.util.List;

@Mapper
public interface NotificationMapper {

    @Select("SELECT * FROM notification WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Notification> findByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = FALSE")
    int countUnread(Long userId);

    @Insert("INSERT INTO notification (user_id, title, content, type) VALUES (#{userId}, #{title}, #{content}, #{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    @Update("UPDATE notification SET is_read = TRUE WHERE id = #{id}")
    int markAsRead(Long id);

    @Update("UPDATE notification SET is_read = TRUE WHERE user_id = #{userId}")
    int markAllAsRead(Long userId);
}

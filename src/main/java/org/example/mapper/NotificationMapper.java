package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.Notification;
import java.util.List;

@Mapper
public interface NotificationMapper {

    @Select("SELECT * FROM notification WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Notification> findByUserId(Long userId);

    @Select("SELECT * FROM notification WHERE id = #{id}")
    Notification findById(Long id);

    @Select("SELECT * FROM notification ORDER BY created_at DESC LIMIT 100")
    List<Notification> findRecent();

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = FALSE")
    int countUnread(Long userId);

    @Insert("INSERT INTO notification (user_id, title, content, type) VALUES (#{userId}, #{title}, #{content}, #{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    @Update("UPDATE notification SET is_read = TRUE WHERE id = #{id}")
    int markAsRead(Long id);

    @Update("UPDATE notification SET is_read = TRUE WHERE id = #{id} AND user_id = #{userId}")
    int markAsReadForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE notification SET is_read = TRUE WHERE user_id = #{userId}")
    int markAllAsRead(Long userId);

    @Delete("DELETE FROM notification WHERE id = #{id}")
    int deleteById(Long id);
}

package com.teach.user.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.teach.user.entity.User;

import java.util.List;

/**
 * 仅操作 user 表。不包含任何跨域 JOIN / 读别服务表（如 course_enrollment）。
 */
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM `user` WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM `user` ORDER BY created_at DESC")
    List<User> findAll();

    @Select("SELECT * FROM `user` WHERE role = #{role} ORDER BY created_at DESC")
    List<User> findByRole(String role);

    @Select("SELECT COUNT(*) FROM `user`")
    int countAll();

    @Select("SELECT COUNT(*) FROM `user` WHERE role = #{role}")
    int countByRole(String role);

    @Insert("INSERT INTO `user` (username, password, role, name, email, status) "
            + "VALUES (#{username}, #{password}, #{role}, #{name}, #{email}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE `user` SET name=#{name}, email=#{email}, avatar_url=#{avatarUrl} WHERE id=#{id}")
    int updateProfile(User user);

    @Update("UPDATE `user` SET name=#{name}, email=#{email}, role=#{role} WHERE id=#{id}")
    int updateByAdmin(User user);

    @Update("UPDATE `user` SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE `user` SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Delete("DELETE FROM `user` WHERE id=#{id}")
    int deleteById(Long id);
}

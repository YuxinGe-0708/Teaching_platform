package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM user ORDER BY created_at DESC")
    List<User> findAll();

    @Select("SELECT * FROM user WHERE role = #{role} ORDER BY created_at DESC")
    List<User> findByRole(String role);

    @Select("SELECT COUNT(*) FROM user")
    int countAll();

    @Select("SELECT COUNT(*) FROM user WHERE role = #{role}")
    int countByRole(String role);

    @Insert("INSERT INTO user (username, password, role, name, email) VALUES (#{username}, #{password}, #{role}, #{name}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET name=#{name}, email=#{email}, avatar_url=#{avatarUrl} WHERE id=#{id}")
    int update(User user);

    @Update("UPDATE user SET name=#{name}, email=#{email}, role=#{role} WHERE id=#{id}")
    int updateByAdmin(User user);

    @Update("UPDATE user SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Delete("DELETE FROM user WHERE id=#{id}")
    int deleteById(Long id);
}

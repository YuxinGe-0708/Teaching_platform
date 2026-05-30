package org.example.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.example.entity.OperationLog;

import java.util.List;

@Mapper
public interface OperationLogMapper {
    @Insert("INSERT INTO operation_log (user_id, username, action, detail) VALUES (#{userId}, #{username}, #{action}, #{detail})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);

    @Select("SELECT * FROM operation_log WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<OperationLog> findByUserId(Long userId);
}

package org.example.mapper;
import org.example.entity.OperationLog;
import java.util.List;
public interface OperationLogMapper {
    int insert(OperationLog value); List<OperationLog> findByUserId(Long userId); List<OperationLog> findRecent();
}

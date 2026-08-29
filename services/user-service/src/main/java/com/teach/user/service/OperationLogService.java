package com.teach.user.service;

import com.teach.user.entity.OperationLog;
import com.teach.user.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 记录操作日志。username 由调用方传入（跨服务无从联查）；
     * 若调用方未传，则本服务依据 userId 回查 user.name 兜底。
     */
    public void record(Long userId, String username, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }

    public List<OperationLog> findByUserId(Long userId) {
        return operationLogMapper.findByUserId(userId);
    }

    public List<OperationLog> findRecent() {
        return operationLogMapper.findRecent();
    }
}

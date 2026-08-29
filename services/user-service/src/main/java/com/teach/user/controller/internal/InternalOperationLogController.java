package com.teach.user.controller.internal;

import com.teach.user.dto.ApiResponse;
import com.teach.user.service.OperationLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间接口：其它服务上报操作日志（统一归口 user-service 存储）。
 */
@RestController
@RequestMapping("/internal/operation-logs")
public class InternalOperationLogController {

    private final OperationLogService operationLogService;

    public InternalOperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @PostMapping
    public ApiResponse<String> record(@RequestParam Long userId,
                                      @RequestParam(required = false) String username,
                                      @RequestParam String action,
                                      @RequestParam(required = false) String detail) {
        operationLogService.record(userId, username, action, detail);
        return ApiResponse.ok("ok", null);
    }
}

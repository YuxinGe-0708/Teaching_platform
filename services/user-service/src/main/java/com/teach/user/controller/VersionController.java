package com.teach.user.controller;

import com.teach.user.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 版本接口：用于部署后查看版本号。 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Value("${info.app.name:user-service}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String version;

    @GetMapping
    public ApiResponse<Map<String, String>> version() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("name", appName);
        data.put("version", version);
        return ApiResponse.ok(data);
    }
}

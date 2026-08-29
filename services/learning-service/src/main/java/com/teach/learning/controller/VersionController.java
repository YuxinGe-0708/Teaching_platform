package com.teach.learning.controller;
import com.teach.learning.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
public class VersionController {
    @GetMapping("/api/version")
    public ApiResponse<Map<String,String>> version() {
        Map<String,String> info = new HashMap<>();
        info.put("service","learning-service"); info.put("version","1.0.0");
        return ApiResponse.ok(info);
    }
}

package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.util.MarkdownUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/markdown")
public class MarkdownController {

    @PostMapping("/render")
    public ApiResponse<Map<String, String>> render(@RequestBody Map<String, String> body) {
        String markdown = body.getOrDefault("markdown", "");
        Map<String, String> data = new HashMap<>();
        data.put("html", MarkdownUtils.toHtml(markdown));
        return ApiResponse.ok(data);
    }
}

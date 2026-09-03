package com.teach.learning.controller;

import com.teach.learning.dto.ApiResponse;
import com.teach.learning.service.AiService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v2/ai")
public class AiController {
    private final AiService ai; public AiController(AiService ai){this.ai=ai;}
    @PostMapping("/chat") public ApiResponse<Map<String,String>> chat(@RequestBody Map<String,String> body){String message=body.get("message");if(message==null||message.trim().isEmpty())return ApiResponse.fail("消息不能为空");String sid=body.getOrDefault("sessionId","anonymous");Map<String,String> out=new HashMap<>();out.put("reply",ai.chat(sid,body.getOrDefault("courseName","通用课程"),message.trim()));return ApiResponse.ok(out);}
    @PostMapping("/clear") public ApiResponse<String> clear(@RequestBody Map<String,String> body){ai.clear(body.getOrDefault("sessionId","anonymous"));return ApiResponse.ok("会话已清除");}
    @PostMapping("/explain-image") public ApiResponse<Map<String,String>> image(@RequestBody Map<String,String> body){Map<String,String> out=new HashMap<>();out.put("reply",ai.explainImage(body.getOrDefault("courseName","通用课程"),body.getOrDefault("resourceTitle","课程视频"),body.getOrDefault("image","")));return ApiResponse.ok(out);}
    @PostMapping("/summarize") public ApiResponse<String> summarize(@RequestBody Map<String,String> body){return ApiResponse.ok(ai.summarize(body.getOrDefault("courseName","通用课程"),body.getOrDefault("resourceTitle","课程资料"),body.getOrDefault("text","")));}
    @PostMapping("/mind-map") public ApiResponse<String> mindMap(@RequestBody Map<String,String> body){return ApiResponse.ok(ai.mindMap(body.getOrDefault("courseName","通用课程"),body.getOrDefault("title","课程笔记"),body.getOrDefault("text","")));}
}

package com.teach.learning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** AI capability owned by learning-service. No page/BFF process calls the provider directly. */
@Service
public class AiService {
    @Value("${app.ai.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}") private String apiUrl;
    @Value("${app.ai.api-key:}") private String apiKey;
    @Value("${app.ai.model:qwen-plus}") private String model;
    @Value("${app.ai.vision-model:qwen3-vl-plus}") private String visionModel;
    private final ObjectMapper json = new ObjectMapper();
    private final RestTemplate http = new RestTemplate();
    private final Map<String,List<Map<String,String>>> sessions = new ConcurrentHashMap<>();

    public String chat(String sessionId,String courseName,String message){
        if(!configured()) return "AI 助手尚未配置 API Key。";
        List<Map<String,String>> history=sessions.computeIfAbsent(sessionId,k->new ArrayList<>());
        if(history.isEmpty()) history.add(msg("system","你是在线教学平台的 AI 助教。当前课程是《"+courseName+"》。请用中文简洁回答。"));
        history.add(msg("user",message));
        try { String reply=complete(model,history); if(reply==null||reply.trim().isEmpty()) return "AI API 返回内容为空，请稍后重试。"; history.add(msg("assistant",reply)); return reply; }
        catch(Exception e){history.remove(history.size()-1); return "AI 调用失败："+e.getMessage();}
    }
    public void clear(String sessionId){sessions.remove(sessionId);}
    public String explainImage(String course,String resource,String image){
        if(!configured()) return "AI 助手尚未配置 API Key。";
        try { ObjectNode body=json.createObjectNode(); body.put("model",visionModel); body.put("max_tokens",800); ArrayNode ms=body.putArray("messages"); ObjectNode u=ms.addObject();u.put("role","user");ArrayNode content=u.putArray("content");ObjectNode t=content.addObject();t.put("type","text");t.put("text","课程："+course+"，视频："+resource+"。请解释截图知识点。");ObjectNode i=content.addObject();i.put("type","image_url");i.putObject("image_url").put("url",image);return call(body); } catch(Exception e){return "AI 图像解释失败："+e.getMessage();}
    }
    public String summarize(String course,String title,String text){if(!configured())return "AI 助手尚未配置 API Key。";String source=text==null?"":text; if(source.length()>12000)source=source.substring(0,12000);try{return complete(model,Arrays.asList(msg("system","你是课程资料整理助手。"),msg("user","课程："+course+"，资料："+title+"。请生成中文知识点概括：核心概念、重点步骤、易错点、复习建议。\n"+source)));}catch(Exception e){return "AI 笔记生成失败："+e.getMessage();}}
    public String mindMap(String course,String title,String text){if(!configured())return "mindmap\n  "+title;try{return complete(model,Arrays.asList(msg("system","只输出 Mermaid mindmap 语法。"),msg("user","课程："+course+"，标题："+title+"。整理为 Mermaid mindmap：\n"+text)));}catch(Exception e){return "mindmap\n  "+title;}}
    private String complete(String selected,List<Map<String,String>> messages)throws Exception{ObjectNode body=json.createObjectNode();body.put("model",selected);body.put("max_tokens",1000);ArrayNode arr=body.putArray("messages");for(Map<String,String> m:messages){ObjectNode n=arr.addObject();n.put("role",m.get("role"));n.put("content",m.get("content"));}return call(body);}
    private String call(ObjectNode body)throws Exception{HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);h.setBearerAuth(apiKey.trim());ResponseEntity<String> r=http.exchange(apiUrl,HttpMethod.POST,new HttpEntity<>(json.writeValueAsString(body),h),String.class);JsonNode choices=json.readTree(r.getBody()).path("choices");return choices.isArray()&&choices.size()>0?choices.get(0).path("message").path("content").asText():"";}
    private Map<String,String> msg(String role,String content){Map<String,String> m=new HashMap<>();m.put("role",role);m.put("content",content);return m;} private boolean configured(){return apiKey!=null&&!apiKey.trim().isEmpty();}
}

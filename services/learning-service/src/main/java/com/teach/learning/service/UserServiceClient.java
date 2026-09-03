package com.teach.learning.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.*;

/** Cross-service adapter. Learning never opens user-service's database. */
@Component
public class UserServiceClient {
    private final RestTemplate restTemplate;
    public UserServiceClient() { SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory(); f.setConnectTimeout(1500); f.setReadTimeout(3000); restTemplate=new RestTemplate(f); }
    @Value("${app.user-service.url:http://localhost:8082}") private String baseUrl;
    @Value("${app.internal-api-key:dev-internal-key}") private String internalApiKey;
    public boolean notify(Long userId, String title, String content, String type) {
        if (userId == null) return false;
        Map<String,Object> body = new HashMap<>(); body.put("userId",userId); body.put("title",title); body.put("content",content); body.put("type",type);
        HttpHeaders h=new HttpHeaders(); h.set("X-Internal-Api-Key",internalApiKey); h.setContentType(MediaType.APPLICATION_JSON); HttpEntity<Map<String,Object>> entity=new HttpEntity<>(body,h);
        for (int i=0;i<3;i++) try { ResponseEntity<String> r=restTemplate.postForEntity(baseUrl+"/internal/notifications",entity,String.class); if(r.getStatusCode().is2xxSuccessful())return true; } catch(RestClientException ignored){ try{Thread.sleep(100L*(i+1));}catch(InterruptedException e){Thread.currentThread().interrupt();break;} }
        return false;
    }
}

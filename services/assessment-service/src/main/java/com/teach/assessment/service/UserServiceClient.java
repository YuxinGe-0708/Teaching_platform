package com.teach.assessment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

/** Notifications and audit records are owned by user-service. */
@Component
public class UserServiceClient {
    private final RestTemplate restTemplate;
    @Value("${app.user-service-url:http://localhost:8082}") private String baseUrl;
    @Value("${app.internal-api-key:dev-internal-key}") private String internalApiKey;
    public UserServiceClient(){SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(1500);f.setReadTimeout(3000);restTemplate=new RestTemplate(f);}
    public boolean notify(Long userId,String title,String content){Map<String,Object>b=new HashMap<>();b.put("userId",userId);b.put("title",title);b.put("content",content);b.put("type","grade");HttpHeaders h=new HttpHeaders();h.set("X-Internal-Api-Key",internalApiKey);h.setContentType(MediaType.APPLICATION_JSON);HttpEntity<Map<String,Object>> e=new HttpEntity<>(b,h);for(int i=0;i<3;i++)try{if(restTemplate.postForEntity(baseUrl+"/internal/notifications",e,String.class).getStatusCode().is2xxSuccessful())return true;}catch(RestClientException ignored){}return false;}
    public boolean log(Long userId,String action,String detail){String u=baseUrl+"/internal/operation-logs?userId="+userId+"&action="+action+"&detail="+detail;HttpHeaders h=new HttpHeaders();h.set("X-Internal-Api-Key",internalApiKey);for(int i=0;i<3;i++)try{if(restTemplate.postForEntity(u,new HttpEntity<Void>(h),String.class).getStatusCode().is2xxSuccessful())return true;}catch(RestClientException ignored){}return false;}
}

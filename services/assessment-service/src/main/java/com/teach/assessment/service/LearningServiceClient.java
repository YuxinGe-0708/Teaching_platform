package com.teach.assessment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.HashSet;

/** Assessment accesses course/enrollment data only through learning-service HTTP APIs. */
@Component
public class LearningServiceClient {
    private final RestTemplate restTemplate;
    @Value("${app.learning-service-url:http://localhost:8083}") private String baseUrl;
    @Value("${app.internal-api-key:dev-internal-key}") private String internalApiKey;
    public LearningServiceClient(){SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(1500);f.setReadTimeout(3000);restTemplate=new RestTemplate(f);}
    public boolean courseExists(Long courseId){return get(baseUrl+"/internal/courses/"+courseId);}
    public boolean hasAccess(Long courseId,Long userId){return get(baseUrl+"/internal/courses/"+courseId+"/access?userId="+userId);}
    public boolean enrolled(Long courseId,Long studentId){return get(baseUrl+"/internal/enrollments/check?studentId="+studentId+"&courseId="+courseId);}
    public Set<Long> enrolledCourseIds(Long studentId){Set<Long> ids=new HashSet<>();HttpHeaders h=new HttpHeaders();h.set("X-Internal-Api-Key",internalApiKey);try{JsonNode root=restTemplate.exchange(baseUrl+"/internal/enrollments/student/"+studentId,org.springframework.http.HttpMethod.GET,new HttpEntity<Void>(h),JsonNode.class).getBody();if(root!=null&&root.has("data"))for(JsonNode n:root.get("data")){long id=n.path("courseId").asLong(0);if(id>0)ids.add(id);}}catch(Exception ignored){}return ids;}
    private boolean get(String url){HttpHeaders h=new HttpHeaders();h.set("X-Internal-Api-Key",internalApiKey);HttpEntity<Void> e=new HttpEntity<>(h);for(int i=0;i<3;i++)try{if(restTemplate.exchange(url,org.springframework.http.HttpMethod.GET,e,String.class).getStatusCode().is2xxSuccessful())return true;}catch(RestClientException ignored){}return false;}
}

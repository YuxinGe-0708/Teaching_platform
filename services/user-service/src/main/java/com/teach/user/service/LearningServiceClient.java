package com.teach.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teach.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/** Resolves course membership through learning-service; never reads its database. */
@Component
public class LearningServiceClient {
    private final RestTemplate restTemplate; private final ObjectMapper mapper=new ObjectMapper();
    @Value("${app.learning-service-url:http://localhost:8083}") private String baseUrl;
    @Value("${app.internal-api-key:dev-internal-key}") private String internalApiKey;
    private final UserService users;
    public LearningServiceClient(UserService users){this.users=users;SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(1500);f.setReadTimeout(3000);restTemplate=new RestTemplate(f);}
    public List<User> studentsForCourse(Long courseId){HttpHeaders h=new HttpHeaders();h.set("X-Internal-Api-Key",internalApiKey);try{JsonNode root=restTemplate.exchange(baseUrl+"/internal/enrollments/course/"+courseId,HttpMethod.GET,new HttpEntity<Void>(h),JsonNode.class).getBody();List<User> result=new ArrayList<>();if(root!=null&&root.has("data"))for(JsonNode n:root.get("data")){long id=n.path("studentId").asLong(0);if(id>0){User u=users.findById(id);if(u!=null)result.add(u);}}return result;}catch(Exception e){return Collections.emptyList();}}
}

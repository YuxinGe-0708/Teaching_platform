package com.teach.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teach.user.entity.User;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class LearningServiceClient {

    private static final Logger log = LoggerFactory.getLogger(LearningServiceClient.class);

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserService users;

    @Value("${app.learning-service-url:http://localhost:8083}")
    private String baseUrl;

    @Value("${app.internal-api-key:dev-internal-key}")
    private String internalApiKey;

    public LearningServiceClient(UserService users) {
        this.users = users;
    }

    @CircuitBreaker(name = "learningService", fallbackMethod = "studentsForCourseFallback")
    public List<User> studentsForCourse(Long courseId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);

        log.info("Calling learning-service for course: {}", courseId);
        JsonNode root = restTemplate.exchange(
            baseUrl + "/internal/enrollments/course/" + courseId,
            HttpMethod.GET,
            new HttpEntity<Void>(headers),
            JsonNode.class
        ).getBody();

        List<User> result = new ArrayList<>();
        if (root != null && root.has("data")) {
            for (JsonNode n : root.get("data")) {
                long id = n.path("studentId").asLong(0);
                if (id > 0) {
                    User u = users.findById(id);
                    if (u != null) result.add(u);
                }
            }
        }
        return result;
    }

    public List<User> studentsForCourseFallback(Long courseId, Throwable t) {
        log.warn("Circuit breaker fallback triggered for learning-service, courseId: {}, error: {}",
            courseId, t.getMessage());
        return Collections.emptyList();
    }
}
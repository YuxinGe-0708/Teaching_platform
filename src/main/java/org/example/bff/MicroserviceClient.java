package org.example.bff;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class MicroserviceClient {
    private final RestTemplate http;
    private final ObjectMapper json;
    private final String internalKey;
    private final String userUrl;
    private final String learningUrl;
    private final String assessmentUrl;

    public MicroserviceClient(RestTemplateBuilder builder, ObjectMapper json,
                              @Value("${app.internal-api-key:dev-internal-key}") String internalKey,
                              @Value("${app.services.user-url:http://localhost:8082}") String userUrl,
                              @Value("${app.services.learning-url:http://localhost:8083}") String learningUrl,
                              @Value("${app.services.assessment-url:http://localhost:8084}") String assessmentUrl) {
        this.http = builder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(60)).build();
        this.json=json; this.internalKey=internalKey; this.userUrl=trim(userUrl); this.learningUrl=trim(learningUrl); this.assessmentUrl=trim(assessmentUrl);
    }

    public String user(String path){return userUrl+path;}
    public String learning(String path){return learningUrl+path;}
    public String assessment(String path){return assessmentUrl+path;}
    public UriComponentsBuilder uri(String base){return UriComponentsBuilder.fromHttpUrl(base);}

    public <T> T get(String url,Class<T> type){return exchange(url,HttpMethod.GET,null,type);}
    public <T> List<T> getList(String url,Class<T> type){
        JsonNode data=data(url,HttpMethod.GET,null);
        if(data==null||data.isNull())return Collections.emptyList();
        JavaType listType=json.getTypeFactory().constructCollectionType(List.class,type);
        return json.convertValue(data,listType);
    }
    public <T> T post(String url,Object body,Class<T> type){return exchange(url,HttpMethod.POST,body,type);}
    public <T> T put(String url,Object body,Class<T> type){return exchange(url,HttpMethod.PUT,body,type);}
    public <T> T delete(String url,Class<T> type){return exchange(url,HttpMethod.DELETE,null,type);}

    /** Upload a multipart file to an internal service and return its data payload. */
    public String upload(String url, MultipartFile file, String fieldName, MultiValueMap<String, Object> fields) {
        if (file == null || file.isEmpty()) return null;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> form = fields == null ? new LinkedMultiValueMap<>() : fields;
        ByteArrayResource resource = new ByteArrayResource(toBytes(file)) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        };
        form.add(fieldName == null ? "file" : fieldName, resource);
        ResponseEntity<JsonNode> response = http.postForEntity(url, new HttpEntity<>(form, headers), JsonNode.class);
        JsonNode root = response.getBody();
        if (root == null) return null;
        int code = root.path("code").asInt(200);
        if (code < 200 || code >= 300) throw new IllegalStateException(root.path("message").asText("微服务调用失败"));
        JsonNode data = root.get("data");
        return data == null || data.isNull() ? null : (data.isTextual() ? data.asText() : data.toString());
    }

    /** Fetch a binary file from an internal service, preserving content metadata. */
    public ResponseEntity<byte[]> file(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalKey);
        ResponseEntity<byte[]> response = http.exchange(url, HttpMethod.GET, new HttpEntity<Void>(headers), byte[].class);
        return response;
    }

    private static byte[] toBytes(MultipartFile file) {
        try { return file.getBytes(); }
        catch (Exception e) { throw new IllegalStateException("读取上传文件失败", e); }
    }

    private <T> T exchange(String url,HttpMethod method,Object body,Class<T> type){
        JsonNode value=data(url,method,body);
        if(value==null||value.isNull())return null;
        return json.convertValue(value,type);
    }
    private JsonNode data(String url,HttpMethod method,Object body){
        HttpHeaders headers=new HttpHeaders();
        headers.set("X-Internal-Api-Key",internalKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response=http.exchange(url,method,new HttpEntity<>(body,headers),JsonNode.class);
        JsonNode root=response.getBody();
        if(root==null)return null;
        int code=root.path("code").asInt(200);
        if(code<200||code>=300)throw new IllegalStateException(root.path("message").asText("微服务调用失败"));
        return root.get("data");
    }
    private static String trim(String value){return value.endsWith("/")?value.substring(0,value.length()-1):value;}
}

package com.teach.assessment.config;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component; import org.springframework.web.servlet.HandlerInterceptor; import javax.servlet.http.*;
@Component public class InternalApiKeyInterceptor implements HandlerInterceptor { @Value("${app.internal-api-key:dev-internal-key}") private String key; public boolean preHandle(HttpServletRequest r,HttpServletResponse s,Object h){if(key.equals(r.getHeader("X-Internal-Api-Key")))return true;s.setStatus(401);return false;} }

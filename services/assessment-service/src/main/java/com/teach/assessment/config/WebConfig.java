package com.teach.assessment.config;
import org.springframework.context.annotation.Configuration; import org.springframework.web.servlet.config.annotation.*;
@Configuration public class WebConfig implements WebMvcConfigurer { private final InternalApiKeyInterceptor i; public WebConfig(InternalApiKeyInterceptor i){this.i=i;} @Override public void addInterceptors(InterceptorRegistry r){r.addInterceptor(i).addPathPatterns("/internal/**");} }

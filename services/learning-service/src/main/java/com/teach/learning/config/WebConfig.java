package com.teach.learning.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final InternalApiKeyInterceptor internalApiKeyInterceptor;
    public WebConfig(InternalApiKeyInterceptor internalApiKeyInterceptor) { this.internalApiKeyInterceptor = internalApiKeyInterceptor; }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
    @Override public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) { registry.addInterceptor(internalApiKeyInterceptor).addPathPatterns("/internal/**"); }
}

package com.teach.assessment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.teach.assessment.mapper")
public class AssessmentServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AssessmentServiceApplication.class, args); }
}

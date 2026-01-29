package com.example.demo.controller;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI demoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Demo 接口文档")
                        .description("Spring Boot + Knife4j 示例接口文档")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Demo")
                                .email("demo@example.com")
                        )
                );
    }
}
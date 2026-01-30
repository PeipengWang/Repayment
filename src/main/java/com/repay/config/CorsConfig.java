package com.repay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置（全局生效）
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 方式1：全局跨域过滤器（推荐，覆盖所有请求）
     */
    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建跨域配置对象
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有域名跨域（生产环境建议指定具体域名，如 "https://xxx.com"）
        config.addAllowedOriginPattern("*");
        // 允许跨域携带Cookie
        config.setAllowCredentials(true);
        // 允许所有请求方法（GET/POST/PUT/DELETE等）
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 暴露响应头（前端可获取）
        config.addExposedHeader("*");
        // 预检请求有效期（秒）
        config.setMaxAge(3600L);

        // 2. 配置跨域路径匹配规则
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 所有接口都允许跨域
        source.registerCorsConfiguration("/**", config);

        // 3. 返回跨域过滤器
        return new CorsFilter(source);
    }

    /**
     * 方式2：WebMvc配置（可选，和过滤器二选一即可）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
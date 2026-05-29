package com.bjpowernode.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许跨域的源
        config.addAllowedOriginPattern("*");
        
        // 允许跨域的请求头
        config.addAllowedHeader("*");
        
        // 允许跨域的请求方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        
        // 是否允许携带cookie
        config.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒），设置为1800秒（30分钟）
        config.setMaxAge(1800L);
        
        // 为所有路径应用配置
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
} 
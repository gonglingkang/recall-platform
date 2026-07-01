package com.recall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置。
 * <p>
 * 放开所有来源（allowedOriginPatterns = "*"），不再维护端口白名单。
 * 原因：开发环境前端端口会顺延（5173/5174/...），逐个维护白名单成本高且易踩坑；
 * 生产环境如需收紧，可改为具体域名。
 *
 * @author recall
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

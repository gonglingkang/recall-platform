package com.recall.security;

import com.recall.common.jwt.JwtUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置：从 application.yml 的 recall.jwt 读取，并暴露 {@link JwtUtils} Bean。
 *
 * @author recall
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "recall.jwt")
public class JwtProperties {

    /** 签名密钥（HMAC-SHA，至少 32 字节） */
    private String secret = "recall-platform-default-secret-please-change-in-production-32bytes";

    /** 过期时间（毫秒），默认 7 天 */
    private long expirationMillis = 7 * 24 * 60 * 60 * 1000L;

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(secret, expirationMillis);
    }
}

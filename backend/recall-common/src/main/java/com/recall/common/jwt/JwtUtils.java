package com.recall.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析工具。
 * <p>
 * Claims 约定：subject=用户名，自定义 claim {@code uid}=用户 ID，{@code exp}=过期时间。
 *
 * @author recall
 */
@Slf4j
public class JwtUtils {

    private final SecretKey key;
    /** 过期时间（毫秒），默认 7 天 */
    private final long expirationMillis;

    public JwtUtils(String secret, long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /**
     * 签发 token。
     *
     * @param userId   用户 ID
     * @param username 用户名
     */
    public String generate(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，失败返回 null。
     */
    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long uid = claims.get("uid", Long.class);
            if (uid == null) {
                // 兼容序列化为 Integer 的情况
                Object raw = claims.get("uid");
                if (raw instanceof Number) {
                    uid = ((Number) raw).longValue();
                }
            }
            return new ParsedToken(uid, claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /** 解析结果 */
    public record ParsedToken(Long userId, String username) {
    }
}

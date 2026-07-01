package com.recall.security;

import com.recall.common.context.UserContext;
import com.recall.common.context.UserContextHolder;
import com.recall.common.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器。
 * <p>
 * 从请求头 {@code Authorization: Bearer <token>} 解析用户，注入：
 * <ul>
 *   <li>{@link UserContextHolder}（供业务层取 userId）</li>
 *   <li>Spring SecurityContext（供后续鉴权判定）</li>
 * </ul>
 * 解析失败不抛异常，交由 Spring Security 的 .authenticated() 规则拦截 → 401。
 *
 * @author recall
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            JwtUtils.ParsedToken parsed = jwtUtils.parse(token);
            if (parsed != null && parsed.userId() != null) {
                UserContext ctx = UserContext.builder()
                        .userId(parsed.userId())
                        .username(parsed.username())
                        .build();
                UserContextHolder.set(ctx);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // 防止线程复用导致用户串号
            UserContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}

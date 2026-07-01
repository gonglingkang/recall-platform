package com.recall.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 当前登录用户上下文。由 {@code JwtAuthFilter} 从 token 解析后注入，业务层通过
 * {@link UserContextHolder} 获取，避免 userId 在各方法间显式传递。
 *
 * @author recall
 */
@Data
@Builder
@AllArgsConstructor
public class UserContext {

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;
}

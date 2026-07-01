package com.recall.common.context;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;

/**
 * 基于 ThreadLocal 的当前用户持有器。
 * <p>
 * 请求进入 {@code JwtAuthFilter} 时 set，请求结束时 clear，防止线程复用导致的用户串号。
 * <p>
 * <b>数据隔离强约束</b>：所有按用户隔离的查询都必须从 {@link #getUserId()} 取当前用户 ID 作为过滤条件，
 * 禁止仅凭资源 ID 访问（见 PRD 3.2 / 13.3）。
 *
 * @author recall
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext ctx) {
        HOLDER.set(ctx);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    /**
     * 获取当前用户 ID，未登录返回 null（用于过滤器、可选登录等不抛异常的场景）。
     *
     * @return 当前用户 ID，未登录时为 null
     */
    public static Long getUserId() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getUserId();
    }

    /**
     * 要求当前已登录并返回用户 ID，未登录抛 401。
     * <p>
     * 业务 Service 统一用本方法取当前用户，避免各 Service 重复写判空 + 抛异常的样板代码。
     *
     * @return 当前用户 ID
     * @throws BusinessException 未登录时抛 UNAUTHORIZED(401)
     */
    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}

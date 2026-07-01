package com.recall.service.auth;

import com.recall.dto.auth.LoginReq;
import com.recall.dto.auth.RegisterReq;
import com.recall.vo.auth.LoginVO;
import com.recall.vo.auth.UserVO;

/**
 * 认证 Service（PRD 6.2 账户模块）。
 *
 * @author recall
 */
public interface AuthService {

    /**
     * 注册：校验唯一性、BCrypt 哈希存储、自动登录、自动生成默认分类。
     *
     * @param req 注册请求
     * @return 登录结果（含 JWT 与用户信息）
     */
    LoginVO register(RegisterReq req);

    /**
     * 登录：账号或邮箱 + 密码，校验通过签发 JWT。
     *
     * @param req 登录请求
     * @return 登录结果（含 JWT 与用户信息）
     */
    LoginVO login(LoginReq req);

    /**
     * 退出：JWT 无状态，前端清除即可；服务端可选加入黑名单(P1，当前为空实现)。
     */
    void logout();

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前登录用户信息
     */
    UserVO currentUser();
}

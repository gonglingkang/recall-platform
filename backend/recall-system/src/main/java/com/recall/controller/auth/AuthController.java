package com.recall.controller.auth;

import com.recall.common.api.Result;
import com.recall.dto.auth.LoginReq;
import com.recall.dto.auth.RegisterReq;
import com.recall.service.auth.AuthService;
import com.recall.vo.auth.LoginVO;
import com.recall.vo.auth.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller（PRD 11.1）。
 *
 * @author recall
 */
@Tag(name = "认证", description = "注册/登录/退出/当前用户")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册", description = "注册成功自动登录，返回 token（PRD 6.2.1）")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterReq req) {
        return Result.ok(authService.register(req));
    }

    @Operation(summary = "登录", description = "账号或邮箱 + 密码，签发 JWT（PRD 6.2.2）")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @Operation(summary = "退出", description = "JWT 无状态，前端清除即可（PRD 6.2.3）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "当前登录用户", description = "获取当前登录用户信息（PRD 11.1）")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.currentUser());
    }
}

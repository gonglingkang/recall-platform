package com.recall.controller.user;

import com.recall.common.api.Result;
import com.recall.dto.user.PasswordReq;
import com.recall.dto.user.PreferencesReq;
import com.recall.dto.user.ProfileReq;
import com.recall.service.user.UserService;
import com.recall.vo.auth.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户设置 Controller（PRD 11.6）。
 *
 * @author recall
 */
@Tag(name = "用户设置", description = "个人资料、修改密码、偏好设置")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "修改资料", description = "昵称/邮箱（PRD 6.9.1）")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileReq req) {
        return Result.ok(userService.updateProfile(req));
    }

    @Operation(summary = "修改密码", description = "需校验原密码（PRD 6.9.2）")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordReq req) {
        userService.updatePassword(req);
        return Result.ok();
    }

    @Operation(summary = "修改偏好", description = "默认分类/提醒开关/时区（PRD 6.9.3）")
    @PutMapping("/preferences")
    public Result<UserVO> updatePreferences(@Valid @RequestBody PreferencesReq req) {
        return Result.ok(userService.updatePreferences(req));
    }
}

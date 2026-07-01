package com.recall.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.auth.UserMapper;
import com.recall.dto.user.PasswordReq;
import com.recall.dto.user.PreferencesReq;
import com.recall.dto.user.ProfileReq;
import com.recall.entity.auth.User;
import com.recall.service.user.UserService;
import com.recall.vo.auth.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户设置 Service 实现（PRD 6.9）。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(ProfileReq req) {
        User user = currentUser();
        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
            // 邮箱唯一校验（PRD 6.9.1）
            if (userMapper.exists(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, req.getEmail())
                    .ne(User::getId, user.getId()))) {
                throw new BusinessException(ResultCode.EMAIL_EXISTS);
            }
            user.setEmail(req.getEmail());
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(PasswordReq req) {
        User user = currentUser();
        // 校验原密码
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_WRONG);
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updatePreferences(PreferencesReq req) {
        User user = currentUser();
        if (req.getReminderEnabled() != null) {
            user.setReminderEnabled(req.getReminderEnabled());
        }
        if (req.getTimezone() != null) {
            user.setTimezone(req.getTimezone());
        }
        // theme 暂存于前端（PRD 6.9.3 P2），后端不持久化
        userMapper.updateById(user);
        return toVO(user);
    }

    private User currentUser() {
        Long userId = UserContextHolder.requireUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return user;
    }

    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .timezone(user.getTimezone())
                .reminderEnabled(user.getReminderEnabled())
                .build();
    }
}

package com.recall.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContext;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.common.jwt.JwtUtils;
import com.recall.dao.auth.UserMapper;
import com.recall.dto.auth.LoginReq;
import com.recall.dto.auth.RegisterReq;
import com.recall.entity.auth.User;
import com.recall.entity.category.Category;
import com.recall.dao.category.CategoryMapper;
import com.recall.service.auth.AuthService;
import com.recall.vo.auth.LoginVO;
import com.recall.vo.auth.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证 Service 实现（PRD 6.2）。
 * <p>
 * 密码 BCrypt 哈希；登录失败不区分账号/密码错误（PRD 8.2 安全考虑）。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /** PRD 6.4.1：新用户默认大分类 */
    private static final List<String> DEFAULT_CATEGORIES = List.of("公司事务", "生活事务", "学习成长");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterReq req) {
        // 确认密码一致性
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "两次密码不一致");
        }
        // 用户名唯一
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()))) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        // 邮箱唯一
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()))) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getUsername());
        user.setTimezone("Asia/Shanghai");
        user.setReminderEnabled(true);
        userMapper.insert(user);

        // PRD 6.4.1：自动创建默认大分类
        int order = 0;
        for (String name : DEFAULT_CATEGORIES) {
            Category c = new Category();
            c.setUserId(user.getId());
            c.setName(name);
            c.setSortOrder(order++);
            categoryMapper.insert(c);
        }

        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginReq req) {
        // 账号或邮箱查询
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getAccount())
                .or()
                .eq(User::getEmail, req.getAccount()));

        // PRD 8.2：不区分账号不存在还是密码错误
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // TODO(P1 PRD 6.2.2): 连续5次失败锁定5分钟，当前简化为直接校验密码
        return buildLoginVO(user);
    }

    @Override
    public void logout() {
        // TODO(P1): 将当前 token 加入黑名单。JWT 无状态下前端清除即可。
    }

    @Override
    public UserVO currentUser() {
        Long userId = UserContextHolder.requireUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .timezone(user.getTimezone())
                .reminderEnabled(user.getReminderEnabled())
                .build();
    }

    private LoginVO buildLoginVO(User user) {
        String token = jwtUtils.generate(user.getId(), user.getUsername());
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
    }
}

package com.recall;

import com.recall.common.context.UserContext;
import com.recall.common.context.UserContextHolder;
import com.recall.dao.auth.UserMapper;
import com.recall.dao.category.CategoryMapper;
import com.recall.entity.auth.User;
import com.recall.entity.category.Category;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试基类：启动完整 Spring 上下文，连接真实 MySQL。
 * 提供创建测试用户并注入认证上下文的工具方法。
 * <p>
 * 各子类用 @Transactional 实现测试后回滚，避免污染数据库。
 *
 * @author recall
 */
@SpringBootTest
public abstract class BaseTest {

    @Autowired
    protected UserMapper userMapper;
    @Autowired
    protected CategoryMapper categoryMapper;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis());

    /**
     * 生成一个短唯一标识（数字串，避免用户名超 20 字符限制）。
     */
    protected String unique() {
        return String.valueOf(SEQ.incrementAndGet());
    }

    /**
     * 创建一个唯一测试用户并注入 UserContext，返回 userId。
     */
    protected Long loginAsNewUser() {
        long n = SEQ.incrementAndGet();
        User user = new User();
        user.setUsername("u" + n);
        user.setEmail("u" + n + "@t.com");
        user.setPasswordHash(passwordEncoder.encode("Test1234"));
        user.setNickname("测试" + n);
        user.setTimezone("Asia/Shanghai");
        user.setReminderEnabled(true);
        userMapper.insert(user);

        UserContext ctx = UserContext.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
        UserContextHolder.set(ctx);
        return user.getId();
    }

    /** 创建一个大分类（归属当前用户） */
    protected Long createCategory(Long userId, String name) {
        Category c = new Category();
        c.setUserId(userId);
        c.setName(name);
        c.setSortOrder(0);
        categoryMapper.insert(c);
        return c.getId();
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }
}

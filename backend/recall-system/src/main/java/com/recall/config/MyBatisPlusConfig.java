package com.recall.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：分页 + 乐观锁插件。
 * <p>
 * <b>数据隔离</b>：见 {@link UserDataIsolationInterceptor}，作为 SQL 内建拦截器在执行阶段注入 userId。
 *
 * @author recall
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(UserDataIsolationInterceptor isolationInterceptor) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页（PRD 8.1：单页加载上限 100 条）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 数据隔离（用户维度，强制带 userId）
        interceptor.addInnerInterceptor(isolationInterceptor);
        return interceptor;
    }
}

package com.recall.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.recall.common.context.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充：创建/更新时间。
 * <p>
 * createTime / updateTime 字段在 insert 时填 createTime+updateTime，update 时刷新 updateTime。
 *
 * @author recall
 */
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /** 便于业务层在需要时获取当前用户（供 Service 显式设置 createdBy 等） */
    @SuppressWarnings("unused")
    private Long currentUserId() {
        return UserContextHolder.getUserId();
    }
}

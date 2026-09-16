package com.recall.service.oa.browser;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OA 浏览器操作用户级互斥锁。
 * <p>
 * OA 为单点登录：同一账号两处并发跑浏览器会互相顶掉会话
 * （实测出现「您的帐号在另一地点登录，您被迫下线」）。
 * 工时同步与考勤抓取共用本锁，保证同一用户的 OA 浏览器操作串行。
 *
 * @author recall
 */
@Component
public class OaUserLockManager {

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 尝试获取该用户的 OA 浏览器锁（非阻塞）。
     *
     * @param userId 用户
     * @return true=拿到锁；false=该用户已有浏览器任务在跑
     */
    public boolean tryLock(Long userId) {
        return lockOf(userId).tryLock();
    }

    /**
     * 释放该用户的 OA 浏览器锁（须与 tryLock 配对，在 finally 中调用）。
     *
     * @param userId 用户
     */
    public void unlock(Long userId) {
        lockOf(userId).unlock();
    }

    private ReentrantLock lockOf(Long userId) {
        return locks.computeIfAbsent(userId, k -> new ReentrantLock());
    }
}

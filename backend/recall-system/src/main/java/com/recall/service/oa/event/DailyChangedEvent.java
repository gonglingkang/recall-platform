package com.recall.service.oa.event;

import java.time.LocalDate;

/**
 * 个人日报/请假变更事件。
 * <p>
 * 日报或请假保存/删除后发布，OA 自动同步监听器消费（auto_sync 开启时同步整周）。
 * 监听侧使用 @TransactionalEventListener(AFTER_COMMIT) 保证读到已提交数据。
 *
 * @param userId 用户
 * @param date   变更日期
 * @author recall
 */
public record DailyChangedEvent(Long userId, LocalDate date) {
}

package com.recall.entity.daily;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 日报项-待办关联实体（v1.0），多对多。
 * <p>
 * 一条工作内容可关联 0~N 个待办；保存日报时随日报项全量覆盖。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_report_item_todos")
public class DailyReportItemTodo extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long itemId;

    private Long todoId;
}

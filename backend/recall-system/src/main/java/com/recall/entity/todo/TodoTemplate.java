package com.recall.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 周期待办模板实体。
 * <p>
 * 定时任务按周期（每天/每周几/每月几号）生成待办；完成门控：last_todo_id 指向的
 * 待办仍为待处理时不生成下一条（待办被删除视为已处理）。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("todo_templates")
public class TodoTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String note;

    private Long categoryId;

    /** 优先级 0低/1中/2高 */
    private String priority;

    /** 周期类型 DAILY/WEEKLY/MONTHLY */
    private String recurrenceType;

    /** WEEKLY:1-7(周一=1,逗号分隔);MONTHLY:1-31;DAILY:* */
    private String recurrenceValue;

    /** 周期日内生成时间点 HH:mm */
    private String recurrenceTime;

    /** 是否启用 0停用/1启用 */
    private Integer enabled;

    /** 下次生成日期 */
    private LocalDate nextDate;

    /** 最近生成的待办ID(完成门控) */
    private Long lastTodoId;
}

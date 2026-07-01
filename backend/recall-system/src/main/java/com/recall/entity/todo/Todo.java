package com.recall.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 待办实体。
 * <p>
 * 时间戳(doneAt/createdAt/updatedAt)UTC 存储。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("todos")
public class Todo extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String note;

    private Long categoryId;

    /** 优先级 high/medium/low */
    private String priority;

    /** 状态 pending/done */
    private String status;

    /** 完成时间(UTC) */
    private LocalDateTime doneAt;
}

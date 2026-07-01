package com.recall.entity.sprint;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 月团队冲刺实体。
 * <p>
 * 冲刺任务可关联多个关键成果 K（通过 sprint_key_results 关联表）；
 * 关联 K 后，K 状态变更会联动同步冲刺状态。按 user_id + month 隔离。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sprint_items")
public class SprintItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 月份 YYYY-MM */
    private String month;

    private String title;

    /** 0未开始/1进行中/2已完成 */
    private String status;

    /** 是否需我介入（需介入才可关联 K） */
    private Boolean needInvolved;

    private String note;
}

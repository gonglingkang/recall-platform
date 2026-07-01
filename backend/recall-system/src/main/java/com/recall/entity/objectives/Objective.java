package com.recall.entity.objectives;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 月度绩效目标 O 实体（v2.0）。
 * <p>
 * 进度/状态/完成时间不存储，查询时由其下 key_results 派生计算。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("objectives")
public class Objective extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 月份 YYYY-MM */
    private String month;

    private String name;

    private String description;
}

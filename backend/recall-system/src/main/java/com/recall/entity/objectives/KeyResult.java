package com.recall.entity.objectives;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 关键成果 K 实体（v2.0），归属于目标 O。
 * <p>
 * completeDate 由后端管理：status→done 填当天，切回非 done 清空。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("key_results")
public class KeyResult extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long objectiveId;

    private String name;

    private String description;

    /** not_started / in_progress / done / cancelled */
    private String status;

    private LocalDate planCompleteDate;

    private LocalDate completeDate;

    /** 取消原因（status 为 cancelled 时填入） */
    private String cancelReason;
}

package com.recall.entity.daily;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 日报工作内容项实体（v1.0），归属于日报。
 * <p>
 * 全量覆盖编辑：保存日报时先删后插，排序按 id 升序（插入顺序）。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_report_items")
public class DailyReportItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long reportId;

    /** 工作内容 */
    private String content;

    /** 进度百分比 0-100 */
    private Integer progress;
}

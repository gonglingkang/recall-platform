package com.recall.entity.daily;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 日报主表实体（v1.0）。
 * <p>
 * 一天一份，主表只记录日期；工作内容存 daily_report_items，关联待办存
 * daily_report_item_todos。report_date 不可为未来（后端校验）。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_reports")
public class DailyReport extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 日报日期（自然日，不可为未来） */
    private LocalDate reportDate;
}

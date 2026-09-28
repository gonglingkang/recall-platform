package com.recall.entity.holiday;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 法定节假日/补班配置实体（全局共享，无用户隔离）。
 * <p>
 * 数据来源：timor 节假日 API 按年拉取缓存；type 含义见 {@link com.recall.enums.HolidayType}。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("holiday_configs")
public class HolidayConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 年份 */
    private Integer year;

    /** 日期 */
    private LocalDate holidayDate;

    /** 类型: 1法定节假日 2补班日 */
    private Integer type;

    /** 名称（如 中秋节/国庆节前补班） */
    private String name;
}

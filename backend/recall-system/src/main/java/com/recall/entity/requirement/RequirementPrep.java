package com.recall.entity.requirement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求上线准备项实体。
 * <p>title=条目名（如 建表语句），content=详细内容（SQL/脚本说明，可空）；
 * 仅「开发完成/验收完成」状态的需求可配置，清单可为空。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirement_preps")
public class RequirementPrep extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long requirementId;

    private String title;

    private String content;

    /** 是否完成 0未完成/1完成 */
    private Integer done;

    private Integer sortOrder;
}

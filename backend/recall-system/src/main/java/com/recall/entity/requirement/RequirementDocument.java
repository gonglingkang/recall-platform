package com.recall.entity.requirement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 需求文档实体（仅外部链接，不存文件本体）。
 * <p>type 存数字码：1原型设计 / 2需求文档 / 3会议纪要。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirement_documents")
public class RequirementDocument extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long requirementId;

    /** 1原型设计 / 2需求文档 / 3会议纪要 */
    private String type;

    private String title;

    private String url;

    /** 文档时间 */
    private LocalDate documentDate;
}

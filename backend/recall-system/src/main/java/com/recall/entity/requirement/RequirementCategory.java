package com.recall.entity.requirement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求分类实体（两级，单表 + parentId 表达层级）。
 * <p>
 * parentId 为 null 表示主分类（第一层）；非空表示子分类（第二层）。
 * 应用层控制最多 2 层；color 仅主分类可用。物理删除。
 * 与待办分类(categories)完全独立，互不影响。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirement_categories")
public class RequirementCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 父分类ID;null=主分类,非空=子分类 */
    private Long parentId;

    private String name;

    /** 颜色;仅主分类可用,子分类强制 null */
    private String color;

    private Integer sortOrder;
}

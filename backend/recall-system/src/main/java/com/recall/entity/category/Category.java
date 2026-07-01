package com.recall.entity.category;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分类实体（PRD 9.2）。两层合并单表，用 parentId 表达层级。
 * <p>
 * parentId 为 null 表示大分类（第一层）；非空表示子分类（第二层）。
 * 应用层控制最多 2 层；color 仅大分类可用。物理删除。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("categories")
public class Category extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 父分类ID;null=大分类,非空=子分类 */
    private Long parentId;

    private String name;

    /** 颜色;仅大分类可用,子分类强制 null */
    private String color;

    private Integer sortOrder;
}

package com.recall.entity.requirement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 需求实体。
 * <p>
 * 需求是需求文档/会议文档的归属载体，可绑定一个关键成果 K（一个 K 可被多个需求绑定）。
 * 绑 K 时，讨论中/进行中/开发完成三态由 K 状态映射驱动；未绑 K 时手动维护。
 * 进入不涉及时解绑 K；进入验收完成/发布完成时保留 K 关联（仅断开联动）。
 * <p>
 * devCompleteDate 由后端管理：绑定的 K 已完成时取 K 的 completeDate，切回非开发完成清空。
 * 验收完成可传验收完成时间与验收人；发布完成可传发布完成时间；进入二者保留 devCompleteDate。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirements")
public class Requirement extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String description;

    /** discussing / not_involved / in_progress / dev_done / acceptance_done / released */
    private String status;

    /** 绑定的关键成果 K（1:1 独占，可空） */
    private Long keyResultId;

    /** 需求主分类 ID（必选，业务层强制非空） */
    private Long categoryId;

    /** 需求子分类 ID（可选） */
    private Long subCategoryId;

    /** 首次需求时间 */
    private LocalDate firstDemandDate;

    private LocalDate devCompleteDate;

    private LocalDate acceptanceDate;

    /** 验收人（姓名，进入验收完成时填入） */
    private String acceptancePerson;

    private LocalDate releaseDate;

    /** 不涉及原因（status 为不涉及时填入） */
    private String cancelReason;
}

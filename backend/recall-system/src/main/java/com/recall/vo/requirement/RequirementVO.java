package com.recall.vo.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求 VO。
 * <p>详情/列表返回；含挂载的文档列表与绑定的关键成果 K 摘要（绑定时填充）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "需求")
public class RequirementVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "需求标题")
    private String title;

    @Schema(description = "需求描述")
    private String description;

    @Schema(description = "状态 0讨论中/1不涉及/2进行中/3开发完成/4验收完成/5发布完成")
    private String status;

    @Schema(description = "绑定的关键成果K的ID")
    private Long keyResultId;

    @Schema(description = "需求主分类ID")
    private Long categoryId;

    @Schema(description = "需求主分类名称")
    private String categoryName;

    @Schema(description = "需求子分类ID")
    private Long subCategoryId;

    @Schema(description = "需求子分类名称")
    private String subCategoryName;

    @Schema(description = "首次需求时间")
    private LocalDate firstDemandDate;

    @Schema(description = "绑定的关键成果K摘要(未绑定时为null)")
    private RequirementKeyResultVO keyResult;

    @Schema(description = "开发完成日期(后端管理)")
    private LocalDate devCompleteDate;

    @Schema(description = "验收完成日期")
    private LocalDate acceptanceDate;

    @Schema(description = "验收人(姓名)")
    private String acceptancePerson;

    @Schema(description = "发布完成日期")
    private LocalDate releaseDate;

    @Schema(description = "不涉及原因(状态为不涉及时填)")
    private String cancelReason;

    @Schema(description = "关联的文档列表")
    private List<RequirementDocumentVO> documents;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

package com.recall.dto.requirement;

import com.recall.enums.RequirementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 需求状态切换请求。
 * <p>
 * 绑 K 时，讨论中/进行中/开发完成三态由 K 驱动，禁止手动改这三态；
 * 手动只能进入不涉及/验收完成/发布完成，进入时后端自动解绑 K。
 * 验收完成可传验收完成时间与验收人；发布完成可传发布完成时间；不传则后端填当天。
 * 发布完成为终态，不可再变更。
 *
 * @author recall
 */
@Data
@Schema(description = "需求状态切换请求")
public class RequirementStatusReq {

    @Schema(description = "状态 discussing/not_involved/in_progress/dev_done/acceptance_done/released")
    @NotNull(message = "状态不能为空")
    private RequirementStatus status;

    @Schema(description = "不涉及原因(仅切到 not_involved 时写入，最长500字符)")
    @Size(max = 500, message = "不涉及原因最长500字符")
    private String cancelReason;

    @Schema(description = "验收完成时间(仅切到 acceptance_done 时写入，不传默认当天)")
    private LocalDate acceptanceDate;

    @Schema(description = "验收人(仅切到 acceptance_done 时写入，姓名，最长100字符)")
    @Size(max = 100, message = "验收人最长100字符")
    private String acceptancePerson;

    @Schema(description = "发布完成时间(仅切到 released 时写入，不传默认当天)")
    private LocalDate releaseDate;
}

package com.recall.dto.sprint;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 冲刺任务关联关键成果请求（全量覆盖）。
 * <p>
 * 传入的 keyResultIds 完全覆盖该冲刺的关联关系；空列表表示取消全部关联。
 * 仅 need_involved=true 时允许操作。
 *
 * @author recall
 */
@Data
@Schema(description = "冲刺关联关键成果请求(全量覆盖)")
public class SprintLinkReq {

    @Schema(description = "关键成果ID列表(空列表=取消全部关联)")
    @NotNull(message = "keyResultIds 不能为 null（空列表=取消全部关联）")
    private List<Long> keyResultIds;
}

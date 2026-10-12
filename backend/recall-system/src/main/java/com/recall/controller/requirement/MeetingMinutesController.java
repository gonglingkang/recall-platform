package com.recall.controller.requirement;

import com.recall.common.api.Result;
import com.recall.dto.requirement.MeetingMinutesBindReq;
import com.recall.dto.requirement.MeetingMinutesQuickCreateReq;
import com.recall.dto.requirement.MeetingMinutesSaveReq;
import com.recall.enums.RequirementStatus;
import com.recall.service.requirement.MeetingMinutesBindRecommendService;
import com.recall.service.requirement.MeetingMinutesPushService;
import com.recall.service.requirement.MeetingMinutesTitleService;
import com.recall.service.requirement.RequirementDocumentService;
import com.recall.service.requirement.RequirementService;
import com.recall.vo.requirement.MeetingMinutesRecommendVO;
import com.recall.vo.requirement.RequirementDocumentVO;
import com.recall.vo.requirement.RequirementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会议纪要 Controller。
 * <p>
 * 纪要 = type=3 的需求文档；未绑定需求（requirement_id NULL）时在纪要池中维护，
 * 绑定后出现在需求详情的文档列表（复用现有展示）。
 * <p>
 * push 端点供外部系统调用（X-Push-Token 鉴权，挂配置的归属用户）；
 * 其余端点为页面用户态接口。
 *
 * @author recall
 */
@Tag(name = "会议纪要", description = "纪要接收(推送/手动)、纪要池维护、绑定需求")
@RestController
@RequestMapping("/api/meeting-minutes")
@RequiredArgsConstructor
public class MeetingMinutesController {

    private final RequirementDocumentService requirementDocumentService;
    private final MeetingMinutesPushService meetingMinutesPushService;
    private final RequirementService requirementService;
    private final MeetingMinutesBindRecommendService meetingMinutesBindRecommendService;
    private final MeetingMinutesTitleService meetingMinutesTitleService;

    @Operation(summary = "生成需求标题", description = "把纪要标题（含日期、会议后缀）由 LLM 转写为简洁的需求标题，供快捷新建需求预填")
    @GetMapping("/generate-requirement-title")
    public Result<String> generateRequirementTitle(
            @Parameter(description = "纪要标题") @RequestParam @NotBlank(message = "纪要标题不能为空") String title) {
        return Result.ok(meetingMinutesTitleService.generateRequirementTitle(title));
    }

    @Operation(summary = "可绑定需求列表", description = "当前用户「讨论中/进行中/开发完成」状态的需求（含分类名），供绑定选择")
    @GetMapping("/bind-options")
    public Result<List<RequirementVO>> bindOptions() {
        return Result.ok(requirementService.listByStatuses(
                java.util.EnumSet.of(RequirementStatus.DISCUSSING, RequirementStatus.IN_PROGRESS, RequirementStatus.DEV_DONE)));
    }

    @Operation(summary = "AI 推荐绑定需求", description = "jev 模型按纪要标题从可绑定状态的需求中推荐最相关的一个（仅供参考）；需 recall.jev.enabled=true")
    @GetMapping("/recommend-requirement")
    public Result<MeetingMinutesRecommendVO> recommendRequirement(
            @Parameter(description = "纪要标题") @RequestParam @NotBlank(message = "纪要标题不能为空") String title) {
        return Result.ok(meetingMinutesBindRecommendService.recommend(title));
    }

    @Operation(summary = "推送会议纪要", description = "外部系统推送入库：X-Push-Token 鉴权，挂配置归属用户，按 URL 幂等（重复推送只更新标题/日期）")
    @PostMapping("/push")
    public Result<RequirementDocumentVO> push(@RequestHeader(value = "X-Push-Token", required = false) String token,
                                              @Valid @RequestBody MeetingMinutesSaveReq req) {
        return Result.ok(meetingMinutesPushService.push(token, req));
    }

    @Operation(summary = "未绑定纪要池", description = "当前用户未绑定需求的会议纪要列表（会议日期倒序）")
    @GetMapping
    public Result<List<RequirementDocumentVO>> listUnbound() {
        return Result.ok(requirementDocumentService.listUnboundMinutes());
    }

    @Operation(summary = "新建需求并绑定", description = "找不到合适需求时现场定义：新建需求（初始讨论中，首提时间当天）并立即绑定该纪要，原子操作")
    @PostMapping("/{id}/create-requirement")
    public Result<RequirementVO> createRequirementAndBind(@Parameter(description = "纪要ID") @PathVariable Long id,
                                                          @Valid @RequestBody MeetingMinutesQuickCreateReq req) {
        return Result.ok(requirementDocumentService.createRequirementAndBindMinute(id, req));
    }

    @Operation(summary = "绑定需求", description = "纪要挂到指定需求，绑定后出现在需求详情文档列表")
    @PutMapping("/{id}/bind")
    public Result<Void> bind(@Parameter(description = "纪要ID") @PathVariable Long id,
                             @Valid @RequestBody MeetingMinutesBindReq req) {
        requirementDocumentService.bindMinute(id, req.getRequirementId());
        return Result.ok();
    }

    @Operation(summary = "解绑回池", description = "requirement_id 置空，纪要回到未绑定池")
    @PutMapping("/{id}/unbind")
    public Result<Void> unbind(@Parameter(description = "纪要ID") @PathVariable Long id) {
        requirementDocumentService.unbindMinute(id);
        return Result.ok();
    }

    @Operation(summary = "修改纪要标题", description = "推送标题不准时手动纠正")
    @PutMapping("/{id}")
    public Result<Void> updateTitle(@Parameter(description = "纪要ID") @PathVariable Long id,
                                    @RequestBody @NotBlank(message = "标题不能为空") @Size(max = 200, message = "标题最长200字符") String title) {
        requirementDocumentService.updateMinuteTitle(id, title);
        return Result.ok();
    }

    @Operation(summary = "删除纪要", description = "误推清理；已绑定的也可直接删除（需求文档列表同步消失）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "纪要ID") @PathVariable Long id) {
        requirementDocumentService.delete(id);
        return Result.ok();
    }
}

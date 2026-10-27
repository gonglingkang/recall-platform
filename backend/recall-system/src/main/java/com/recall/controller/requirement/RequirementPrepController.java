package com.recall.controller.requirement;

import com.recall.common.api.Result;
import com.recall.dto.requirement.RequirementPrepCreateReq;
import com.recall.dto.requirement.RequirementPrepUpdateReq;
import com.recall.service.requirement.RequirementPrepService;
import com.recall.vo.requirement.RequirementPrepVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 需求上线准备项 Controller。
 * <p>
 * 仅「开发完成/验收完成」状态的需求可配置；清单可为空（空态提供按模板生成 / 手动添加）。
 *
 * @author recall
 */
@Tag(name = "需求上线准备", description = "上线前准备项清单（建表/数据初始化/脚本接口等）")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RequirementPrepController {

    private final RequirementPrepService requirementPrepService;

    @Operation(summary = "上线准备清单", description = "按排序返回需求的上线准备项（可为空列表）")
    @GetMapping("/requirements/{id}/preps")
    public Result<List<RequirementPrepVO>> list(@Parameter(description = "需求ID") @PathVariable Long id) {
        return Result.ok(requirementPrepService.listByRequirement(id));
    }

    @Operation(summary = "新增准备项", description = "手动添加一条上线准备项")
    @PostMapping("/requirements/{id}/preps")
    public Result<RequirementPrepVO> create(@Parameter(description = "需求ID") @PathVariable Long id,
                                            @Valid @RequestBody RequirementPrepCreateReq req) {
        return Result.ok(requirementPrepService.create(id, req));
    }

    @Operation(summary = "更新准备项", description = "改名称/内容（可贴 SQL/脚本说明）/完成状态")
    @PutMapping("/requirement-preps/{prepId}")
    public Result<RequirementPrepVO> update(@Parameter(description = "准备项ID") @PathVariable Long prepId,
                                            @Valid @RequestBody RequirementPrepUpdateReq req) {
        return Result.ok(requirementPrepService.update(prepId, req));
    }

    @Operation(summary = "删除准备项", description = "删除一条上线准备项")
    @DeleteMapping("/requirement-preps/{prepId}")
    public Result<Void> delete(@Parameter(description = "准备项ID") @PathVariable Long prepId) {
        requirementPrepService.delete(prepId);
        return Result.ok();
    }
}

package com.recall.controller.objectives;

import com.recall.common.api.Result;
import com.recall.dto.objectives.ObjectiveCreateReq;
import com.recall.dto.objectives.ObjectiveImportParseReq;
import com.recall.dto.objectives.ObjectiveImportReq;
import com.recall.dto.objectives.ObjectiveUpdateReq;
import com.recall.service.objectives.ObjectiveImportService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.vo.objectives.ObjectiveImportPreviewVO;
import com.recall.vo.objectives.ObjectiveImportResultVO;
import com.recall.vo.objectives.ObjectiveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 月度绩效目标 O Controller（v2.0）。
 * <p>
 * 仅维护目标 O 的接口；关键成果 K 的接口在 KeyResultController。
 *
 * @author recall
 */
@Tag(name = "月度绩效目标", description = "目标O管理，O的进度/完成时间由K派生计算")
@RestController
@RequestMapping("/api/objectives")
@RequiredArgsConstructor
@Validated
public class ObjectiveController {

    private final ObjectiveService objectiveService;
    private final ObjectiveImportService objectiveImportService;

    @Operation(summary = "目标列表", description = "某月目标列表，每个O内嵌其下所有K，派生字段已算好")
    @GetMapping
    public Result<List<ObjectiveVO>> list(@Parameter(description = "月份 YYYY-MM")
                                          @RequestParam @NotBlank(message = "月份不能为空")
                                          @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
                                          String month) {
        return Result.ok(objectiveService.list(month));
    }

    @Operation(summary = "存在目标的月份清单", description = "当前用户存在目标O的月份去重列表（倒序），用于月份搜索")
    @GetMapping("/months")
    public Result<List<String>> listMonths() {
        return Result.ok(objectiveService.listMonths());
    }

    @Operation(summary = "新增目标", description = "名称在同用户同月内唯一；仅 name/description/month")
    @PostMapping
    public Result<ObjectiveVO> create(@Valid @RequestBody ObjectiveCreateReq req) {
        return Result.ok(objectiveService.create(req));
    }

    @Operation(summary = "编辑目标", description = "仅 name/description，派生字段与月份不可改")
    @PutMapping("/{id}")
    public Result<ObjectiveVO> update(@Parameter(description = "目标ID") @PathVariable Long id,
                                      @Valid @RequestBody ObjectiveUpdateReq req) {
        return Result.ok(objectiveService.update(id, req));
    }

    @Operation(summary = "删除目标", description = "连带删除其下所有关键成果K")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "目标ID") @PathVariable Long id) {
        objectiveService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "解析绩效表格文本", description = "LLM 把复制的绩效表内容解析为结构化预览（不写库），供导入确认编辑")
    @PostMapping("/import/parse")
    public Result<List<ObjectiveImportPreviewVO>> parseImport(@Valid @RequestBody ObjectiveImportParseReq req) {
        return Result.ok(objectiveImportService.parse(req));
    }

    @Operation(summary = "批量导入绩效目标", description = "按预览结构批量创建月度目标与关键成果；replace=true 先清空该月全部已有目标（含已完成，须前端已确认）")
    @PostMapping("/import")
    public Result<ObjectiveImportResultVO> importObjectives(@Valid @RequestBody ObjectiveImportReq req) {
        return Result.ok(objectiveImportService.importObjectives(req));
    }
}

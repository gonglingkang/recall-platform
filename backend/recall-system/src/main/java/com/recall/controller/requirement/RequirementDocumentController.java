package com.recall.controller.requirement;

import com.recall.common.api.Result;
import com.recall.dto.requirement.RequirementDocumentCreateReq;
import com.recall.service.requirement.RequirementDocumentService;
import com.recall.vo.requirement.RequirementDocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 需求文档 Controller。
 * <p>
 * 新增文档挂在需求路径下（POST /api/requirements/{id}/documents）；
 * 删除用独立路径（/api/requirement-documents/{docId}）。
 *
 * @author recall
 */
@Tag(name = "需求文档", description = "需求文档的新增、删除（仅外部链接）")
@RestController
@RequiredArgsConstructor
public class RequirementDocumentController {

    private final RequirementDocumentService requirementDocumentService;

    @Operation(summary = "新增需求文档", description = "挂在指定需求下；type区分原型设计/需求文档/会议纪要")
    @PostMapping("/api/requirements/{id}/documents")
    public Result<RequirementDocumentVO> create(@Parameter(description = "需求ID") @PathVariable Long id,
                                                @Valid @RequestBody RequirementDocumentCreateReq req) {
        return Result.ok(requirementDocumentService.create(id, req));
    }

    @Operation(summary = "删除需求文档")
    @DeleteMapping("/api/requirement-documents/{docId}")
    public Result<Void> delete(@Parameter(description = "文档ID") @PathVariable Long docId) {
        requirementDocumentService.delete(docId);
        return Result.ok();
    }
}

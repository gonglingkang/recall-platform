package com.recall.controller.performance;

import com.recall.common.api.Result;
import com.recall.dto.performance.PerformanceReviewGenerateReq;
import com.recall.service.performance.PerformanceReviewService;
import com.recall.vo.performance.PerformanceReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 绩效面谈 Controller。
 * <p>
 * 仅生成面谈内容供用户复制到 OA 面谈表，不做 OA 同步、不落库。
 *
 * @author recall
 */
@Tag(name = "绩效面谈", description = "基于月度目标与日报素材，调用大模型生成面谈四部分内容")
@RestController
@RequestMapping("/api/performance-review")
@RequiredArgsConstructor
public class PerformanceReviewController {

    private final PerformanceReviewService performanceReviewService;

    @Operation(summary = "生成面谈内容", description = "按月汇总绩效目标与日报工作内容作为素材，调用大模型生成四部分内容（模型生成约需数秒至数十秒）")
    @PostMapping("/generate")
    public Result<PerformanceReviewVO> generate(@Valid @RequestBody PerformanceReviewGenerateReq req) {
        return Result.ok(performanceReviewService.generate(req.getMonth()));
    }
}

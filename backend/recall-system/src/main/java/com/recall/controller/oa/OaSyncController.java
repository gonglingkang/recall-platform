package com.recall.controller.oa;

import com.recall.common.api.Result;
import com.recall.common.context.UserContextHolder;
import com.recall.enums.OaSyncTriggerType;
import com.recall.service.oa.OaSyncService;
import com.recall.vo.oa.OaSyncLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * OA 同步 Controller。
 * <p>
 * 同步以周为单位（与 OA 工时表单一致）；date 传该周任一日期，
 * 后端按平台周（周一为起始）归一。同步为异步执行，前端轮询 status。
 *
 * @author recall
 */
@Tag(name = "OA 同步", description = "日报同步到 OA 工时日填报：触发(手动)、状态轮询、日志")
@RestController
@RequestMapping("/api/oa-sync")
@RequiredArgsConstructor
public class OaSyncController {

    private final OaSyncService oaSyncService;

    @Operation(summary = "手动触发同步", description = "异步执行（约1-3分钟），同周进行中返回 4802；只暂存待办不提交")
    @PostMapping("/weeks/{date}/trigger")
    public Result<OaSyncLogVO> trigger(@Parameter(description = "该周任一日期 YYYY-MM-DD")
                                       @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long userId = UserContextHolder.requireUserId();
        return Result.ok(oaSyncService.startSync(userId, date, OaSyncTriggerType.MANUAL));
    }

    @Operation(summary = "查询某周同步状态", description = "返回该周最新一条同步日志；从未同步返回 data=null")
    @GetMapping("/weeks/{date}/status")
    public Result<OaSyncLogVO> status(@Parameter(description = "该周任一日期 YYYY-MM-DD")
                                      @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long userId = UserContextHolder.requireUserId();
        return Result.ok(oaSyncService.getStatus(userId, date));
    }

    @Operation(summary = "最近同步日志", description = "按开始时间倒序，默认10条")
    @GetMapping("/logs")
    public Result<List<OaSyncLogVO>> logs(@Parameter(description = "条数")
                                          @RequestParam(defaultValue = "10") int limit) {
        Long userId = UserContextHolder.requireUserId();
        return Result.ok(oaSyncService.listLogs(userId, limit));
    }
}

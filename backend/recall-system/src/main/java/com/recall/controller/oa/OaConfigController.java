package com.recall.controller.oa;

import com.recall.common.api.Result;
import com.recall.dto.oa.OaConfigSaveReq;
import com.recall.service.oa.OaConfigService;
import com.recall.vo.oa.OaConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OA 对接配置 Controller。
 * <p>
 * 配置按当前登录用户隔离；密码不回明文。
 *
 * @author recall
 */
@Tag(name = "OA 对接配置", description = "OA 地址/账号/密码/项目名/自动同步开关的查看与保存")
@RestController
@RequestMapping("/api/oa-config")
@RequiredArgsConstructor
public class OaConfigController {

    private final OaConfigService oaConfigService;

    @Operation(summary = "查看当前用户 OA 配置", description = "未配置返回 data=null；密码只回 hasPassword")
    @GetMapping
    public Result<OaConfigVO> get() {
        return Result.ok(oaConfigService.getByUserId());
    }

    @Operation(summary = "保存 OA 配置", description = "upsert；password 已配置后留空表示不修改")
    @PutMapping
    public Result<OaConfigVO> save(@Valid @RequestBody OaConfigSaveReq req) {
        return Result.ok(oaConfigService.save(req));
    }
}

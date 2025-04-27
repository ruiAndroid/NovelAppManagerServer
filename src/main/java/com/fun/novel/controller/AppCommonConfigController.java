package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.service.AppCommonConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "应用通用配置接口")
@RestController
@RequestMapping("/api/novel-common")
@RequiredArgsConstructor
public class AppCommonConfigController {

    private final AppCommonConfigService appCommonConfigService;

    @PostMapping("/createAppCommonConfig")
    @Operation(summary = "创建应用通用配置", description = "创建新的应用通用配置")
    public Result<AppCommonConfig> createAppCommonConfig(
            @Parameter(description = "应用通用配置信息", required = true)
            @Valid @RequestBody AppCommonConfigDTO dto) {
        try {
            AppCommonConfig config = appCommonConfigService.createAppCommonConfig(dto);
            return Result.success("创建成功", config);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/getAppCommonConfig")
    @Operation(summary = "获取应用通用配置", description = "根据appid获取应用通用配置信息")
    public Result<AppCommonConfig> getAppCommonConfig(
            @Parameter(description = "小程序ID", required = true)
            @RequestParam String appId) {
        AppCommonConfig config = appCommonConfigService.getAppCommonConfig(appId);
        if (config == null) {
            return Result.error("未找到对应的应用配置");
        }
        return Result.success("获取成功", config);
    }

    @GetMapping("/deleteAppCommonConfig")
    @Operation(summary = "删除应用通用配置", description = "根据appId删除应用通用配置")
    public Result<String> deleteAppCommonConfig(
            @Parameter(description = "小程序ID", required = true)
            @RequestParam String appId) {
        try {
            boolean success = appCommonConfigService.deleteAppCommonConfig(appId);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("未找到对应的应用配置");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/updateAppCommonConfig")
    @Operation(summary = "更新应用通用配置", description = "更新应用通用配置信息")
    public Result<AppCommonConfig> updateAppCommonConfig(
            @Parameter(description = "应用通用配置信息", required = true)
            @Valid @RequestBody AppCommonConfigDTO dto) {
        try {
            AppCommonConfig config = appCommonConfigService.updateAppCommonConfig(dto);
            return Result.success("更新成功", config);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}

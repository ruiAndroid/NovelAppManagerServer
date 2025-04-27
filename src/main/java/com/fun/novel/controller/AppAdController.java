package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.entity.AppAd;
import com.fun.novel.entity.AdConfig;
import com.fun.novel.service.AppAdService;
import com.fun.novel.service.AdConfigService;
import com.fun.novel.dto.AppAdWithConfigDTO;
import com.fun.novel.dto.UpdateAdConfigRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/novel-ad")
@Tag(name = "广告管理", description = "小程序广告和广告配置管理接口")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AppAdController {

    @Autowired
    private AppAdService appAdService;

    @Autowired
    private AdConfigService adConfigService;

    // AppAd相关接口
    @PostMapping("/appAd/create")
    @Operation(summary = "创建AppAd", description = "创建新的AppAd记录")
    public Result<AppAd> createAppAd(
            @Parameter(description = "AppAd对象", required = true)
            @Valid @RequestBody AppAd appAd) {
        try {
            AppAd createdAppAd = appAdService.addAppAd(appAd);
            return Result.success("创建成功", createdAppAd);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/appAd/getAppAdByAppId")
    @Operation(summary = "获取AppAd", description = "根据appId获取AppAd记录")
    public Result<AppAdWithConfigDTO> getAppAdByAppId(
            @Parameter(description = "AppAd ID", required = true)
            @RequestParam String appId) {
        AppAdWithConfigDTO appAdWithConfig = appAdService.getAppAdByAppId(appId);
        if (appAdWithConfig == null) {
            return Result.error("未找到对应的AppAd记录");
        }
        return Result.success("获取成功", appAdWithConfig);
    }

    @GetMapping("/appAd/deleteAppAdByAppId")
    @Operation(summary = "删除AppAd", description = "根据appId删除AppAd")
    public Result<String> deleteAppAdByAppId(
            @Parameter(description = "AppAd ID", required = true)
            @RequestParam String appId) {
        boolean success = appAdService.deleteAppAdByAppId(appId);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("未找到对应的AppAd记录");
        }
    }



    // AdConfig相关接口
    @PostMapping("/adConfig/create")
    @Operation(summary = "创建AdConfig", description = "创建新的AdConfig记录")
    public Result<AdConfig> createAdConfig(
            @Parameter(description = "AdConfig对象", required = true)
            @Valid @RequestBody AdConfig adConfig) {
        try {
            AdConfig createdAdConfig = adConfigService.addAdConfig(adConfig);
            return Result.success("创建成功", createdAdConfig);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }


    @PostMapping("/adConfig/update")
    @Operation(summary = "更新AdConfig", description = "更新广告配置信息")
    public Result<AdConfig> updateAdConfig(
            @Parameter(description = "更新广告配置请求", required = true)
            @Valid @RequestBody UpdateAdConfigRequest request) {
        try {
            AdConfig updatedAdConfig = adConfigService.updateAdConfig(request);
            return Result.success("更新成功", updatedAdConfig);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/adConfig/deleteByAppAdIdAndType")
    @Operation(summary = "删除AdConfig", description = "根据appAdId和广告类型删除广告配置")
    public Result<String> deleteAdConfigByAppAdIdAndType(
            @Parameter(description = "应用广告ID", required = true)
            @RequestParam Integer appAdId,
            @Parameter(description = "广告类型", required = true)
            @RequestParam String adType) {
        try {
            boolean success = adConfigService.deleteAdConfigByAppAdIdAndType(appAdId, adType);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("未找到对应的广告配置记录");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

} 
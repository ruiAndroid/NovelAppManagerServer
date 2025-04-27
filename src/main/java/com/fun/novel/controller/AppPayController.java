package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.service.AppPayService;
import com.fun.novel.dto.CreateAppPayRequest;
import com.fun.novel.dto.UpdateAppPayRequest;
import com.fun.novel.dto.AppPayWithConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/novel-pay")
@Tag(name = "支付管理", description = "小程序支付配置管理接口")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AppPayController {

    @Autowired
    private AppPayService appPayService;

    @PostMapping("/create")
    @Operation(summary = "创建支付配置", description = "创建新的小程序支付配置，payType必须为：normalPay, orderPay, renewPay, douzuanPay之一")
    public Result<AppPayWithConfigDTO> createAppPay(
            @Parameter(description = "支付配置信息", required = true)
            @Valid @RequestBody CreateAppPayRequest request) {
        try {
            AppPayWithConfigDTO createdAppPay = appPayService.createAppPay(request);
            return Result.success("创建成功", createdAppPay);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/getAppPayByAppId")
    @Operation(summary = "获取支付配置", description = "根据appid获取所有支付类型的配置信息")
    public Result<AppPayWithConfigDTO> getAppPayByAppId(
            @Parameter(description = "小程序ID", required = true)
            @RequestParam String appId) {
        AppPayWithConfigDTO appPayConfig = appPayService.getAppPayByAppId(appId);
        if (appPayConfig == null) {
            return Result.error("未找到对应的支付配置");
        }
        return Result.success("获取成功", appPayConfig);
    }

    @PostMapping("/updateAppPay")
    @Operation(summary = "更新支付配置", description = "更新小程序支付配置，payType必须为：normalPay, orderPay, renewPay, douzuanPay之一")
    public Result<AppPayWithConfigDTO> updateAppPay(
            @Parameter(description = "支付配置信息", required = true)
            @Valid @RequestBody UpdateAppPayRequest request) {
        try {
            AppPayWithConfigDTO updatedAppPay = appPayService.updateAppPay(request);
            return Result.success("更新成功", updatedAppPay);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/deleteAppPayByAppIdAndType")
    @Operation(summary = "删除支付配置", description = "根据appId和支付类型删除支付配置")
    public Result<String> deleteAppPayByAppIdAndType(
            @Parameter(description = "小程序ID", required = true)
            @RequestParam String appId,
            @Parameter(description = "支付类型", required = true)
            @RequestParam String payType) {
        try {
            boolean success = appPayService.deleteAppPayByAppIdAndType(appId, payType);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("未找到对应的支付配置");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
} 
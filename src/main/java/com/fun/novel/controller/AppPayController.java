package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.dto.*;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.service.AppCommonConfigService;
import com.fun.novel.service.AppPayService;
import com.fun.novel.service.NovelAppLocalFileOperationService;
import com.fun.novel.service.NovelAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/novel-pay")
@Tag(name = "支付管理", description = "小程序支付配置管理接口")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AppPayController {

    @Autowired
    private AppPayService appPayService;

    @Autowired
    private NovelAppLocalFileOperationService localFileOperationService;

    @Autowired
    private NovelAppService novelAppService;

    @Autowired
    private AppCommonConfigService appCommonConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建支付配置", description = "创建新的小程序支付配置，payType必须为：normalPay, orderPay, renewPay, douzuanPay之一")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    public Result<AppPayWithConfigDTO> createAppPay(
            @Parameter(description = "支付配置信息", required = true)
            @Valid @RequestBody CreateAppPayRequest request) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();
        try {
            AppPayWithConfigDTO createdAppPay = appPayService.createAppPay(request);
            NovelApp novelApp = novelAppService.getByAppId(createdAppPay.getAppId());
            AppPayWithConfigDTO appPayByAppId = appPayService.getAppPayByAppId(createdAppPay.getAppId());
            CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequest(appPayByAppId, novelApp);
            localFileOperationService.updatePayConfigLocalCodeFiles(createNovelAppRequest,rollbackActions);

            return Result.success("创建成功", createdAppPay);
        } catch (IllegalArgumentException e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
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
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    public Result<AppPayWithConfigDTO> updateAppPay(
            @Parameter(description = "支付配置信息", required = true)
            @Valid @RequestBody UpdateAppPayRequest request) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();
        try {
            // 1. 数据库操作
            AppPayWithConfigDTO updatedAppPay = appPayService.updateAppPay(request);
            // 2. 文件操作
            NovelApp novelApp = novelAppService.getByAppId(updatedAppPay.getAppId());
            AppPayWithConfigDTO appPayByAppId = appPayService.getAppPayByAppId(updatedAppPay.getAppId());
            CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequest(appPayByAppId, novelApp);
            localFileOperationService.updatePayConfigLocalCodeFiles(createNovelAppRequest,rollbackActions);
            return Result.success("更新成功", updatedAppPay);
        } catch (Exception e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/deleteAppPayByAppIdAndType")
    @Operation(summary = "删除支付配置", description = "根据appId和支付类型删除支付配置")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    public Result<String> deleteAppPayByAppIdAndType(
            @Parameter(description = "小程序ID", required = true)
            @RequestParam String appId,
            @Parameter(description = "支付类型", required = true)
            @RequestParam String payType) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();

        try {
            boolean success = appPayService.deleteAppPayByAppIdAndType(appId, payType);
            if (success) {
                NovelApp novelApp = novelAppService.getByAppId(appId);
                AppPayWithConfigDTO appPayByAppId = appPayService.getAppPayByAppId(appId);
                CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequest(appPayByAppId, novelApp);
                localFileOperationService.updatePayConfigLocalCodeFiles(createNovelAppRequest,rollbackActions);


                return Result.success("删除成功");
            } else {
                return Result.error("未找到对应的支付配置");
            }
        } catch (IllegalArgumentException e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error(e.getMessage());
        }
    }


    private CreateNovelAppRequest convertToCreateNovelAppRequest(AppPayWithConfigDTO appPayWithConfigDTO, NovelApp novelApp) {
        CreateNovelAppRequest req = new CreateNovelAppRequest();
        CreateNovelAppRequest.BaseConfig baseConfig = new CreateNovelAppRequest.BaseConfig();
        baseConfig.setAppName(novelApp.getAppName());
        baseConfig.setAppCode(novelApp.getAppCode());
        baseConfig.setPlatform(novelApp.getPlatform());
        baseConfig.setVersion(novelApp.getVersion());
        baseConfig.setProduct(novelApp.getProduct());
        baseConfig.setCustomer(novelApp.getCustomer());
        baseConfig.setAppid(novelApp.getAppid());
        baseConfig.setTokenId(novelApp.getTokenId());
        baseConfig.setCl(novelApp.getCl());
        baseConfig.setMainTheme(novelApp.getMainTheme());
        baseConfig.setSecondTheme(novelApp.getSecondTheme());
        req.setBaseConfig(baseConfig);

        CreateNovelAppRequest.CommonConfig commonConfig = new CreateNovelAppRequest.CommonConfig();
        // buildCode从通用配置查库获取
        AppCommonConfig dbCommonConfig = appCommonConfigService.getAppCommonConfig(novelApp.getAppid());
        if (dbCommonConfig != null) {
            commonConfig.setBuildCode(dbCommonConfig.getBuildCode());
            commonConfig.setContact(dbCommonConfig.getContact());
            commonConfig.setDouyinImId(dbCommonConfig.getDouyinImId());
            commonConfig.setKuaishouAppToken(dbCommonConfig.getKuaishouAppToken());
            commonConfig.setKuaishouClientId(dbCommonConfig.getKuaishouClientId());
            commonConfig.setKuaishouClientSecret(dbCommonConfig.getKuaishouClientSecret());
            commonConfig.setMineLoginType(dbCommonConfig.getMineLoginType());
            commonConfig.setPayCardStyle(dbCommonConfig.getPayCardStyle());
            commonConfig.setHomeCardStyle(dbCommonConfig.getHomeCardStyle());
            commonConfig.setReaderLoginType(dbCommonConfig.getReaderLoginType());
            commonConfig.setWeixinAppToken(dbCommonConfig.getWeixinAppToken());
            commonConfig.setDouyinAppToken(dbCommonConfig.getDouyinAppToken());
            commonConfig.setIaaMode(dbCommonConfig.getIaaMode());
            commonConfig.setIaaDialogStyle(dbCommonConfig.getIaaDialogStyle());

        }
        req.setCommonConfig(commonConfig);

        CreateNovelAppRequest.PaymentConfig paymentConfig = new CreateNovelAppRequest.PaymentConfig();
        if (appPayWithConfigDTO != null) {
            // normalPay
            if (appPayWithConfigDTO.getNormalPay() != null) {
                CreateNovelAppRequest.PayTypeConfig normalPay = new CreateNovelAppRequest.PayTypeConfig();
                normalPay.setEnabled(appPayWithConfigDTO.getNormalPay().getEnabled());
                normalPay.setGatewayAndroid(appPayWithConfigDTO.getNormalPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getNormalPay().getGatewayAndroid().toString());
                normalPay.setGatewayIos(appPayWithConfigDTO.getNormalPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getNormalPay().getGatewayIos().toString());
                paymentConfig.setNormalPay(normalPay);
            }
            // orderPay
            if (appPayWithConfigDTO.getOrderPay() != null) {
                CreateNovelAppRequest.PayTypeConfig orderPay = new CreateNovelAppRequest.PayTypeConfig();
                orderPay.setEnabled(appPayWithConfigDTO.getOrderPay().getEnabled());
                orderPay.setGatewayAndroid(appPayWithConfigDTO.getOrderPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getOrderPay().getGatewayAndroid().toString());
                orderPay.setGatewayIos(appPayWithConfigDTO.getOrderPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getOrderPay().getGatewayIos().toString());
                paymentConfig.setOrderPay(orderPay);
            }
            // renewPay
            if (appPayWithConfigDTO.getRenewPay() != null) {
                CreateNovelAppRequest.PayTypeConfig renewPay = new CreateNovelAppRequest.PayTypeConfig();
                renewPay.setEnabled(appPayWithConfigDTO.getRenewPay().getEnabled());
                renewPay.setGatewayAndroid(appPayWithConfigDTO.getRenewPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getRenewPay().getGatewayAndroid().toString());
                renewPay.setGatewayIos(appPayWithConfigDTO.getRenewPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getRenewPay().getGatewayIos().toString());
                paymentConfig.setRenewPay(renewPay);
            }
            // douzuanPay
            if (appPayWithConfigDTO.getDouzuanPay() != null) {
                CreateNovelAppRequest.PayTypeConfig douzuanPay = new CreateNovelAppRequest.PayTypeConfig();
                douzuanPay.setEnabled(appPayWithConfigDTO.getDouzuanPay().getEnabled());
                douzuanPay.setGatewayAndroid(appPayWithConfigDTO.getDouzuanPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getDouzuanPay().getGatewayAndroid().toString());
                douzuanPay.setGatewayIos(appPayWithConfigDTO.getDouzuanPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getDouzuanPay().getGatewayIos().toString());
                paymentConfig.setDouzuanPay(douzuanPay);
            }
            // wxVirtualPay
            if (appPayWithConfigDTO.getWxVirtualPay() != null) {
                CreateNovelAppRequest.PayTypeConfig wxVirtualPay = new CreateNovelAppRequest.PayTypeConfig();
                wxVirtualPay.setEnabled(appPayWithConfigDTO.getWxVirtualPay().getEnabled());
                wxVirtualPay.setGatewayAndroid(appPayWithConfigDTO.getWxVirtualPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getWxVirtualPay().getGatewayAndroid().toString());
                wxVirtualPay.setGatewayIos(appPayWithConfigDTO.getWxVirtualPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getWxVirtualPay().getGatewayIos().toString());
                paymentConfig.setWxVirtualPay(wxVirtualPay);
            }
        }
        req.setPaymentConfig(paymentConfig);

        CreateNovelAppRequest.DeliverConfig deliverConfig = new CreateNovelAppRequest.DeliverConfig();
        deliverConfig.setDeliverId(novelApp.getDeliverId());
        deliverConfig.setBannerId(novelApp.getBannerId());
        req.setDeliverConfig(deliverConfig);
        // 其它配置如有需要可补充
        return req;
    }
} 
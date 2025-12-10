package com.fun.novel.controller;

import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.dto.*;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.enums.OpType;
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

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

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
    @OperationLog(opType = OpType.INSERT_CODE, opName = "创建新的小程序支付配置")
    public Result<AppPayWithConfigDTO> createAppPay(
            @Parameter(description = "支付配置信息", required = true)
            @Valid @RequestBody CreateAppPayRequest request) {
        List<Runnable> rollbackActions = new ArrayList<>();
        try {
            AppPayWithConfigDTO createdAppPay = appPayService.createAppPay(request);
            NovelApp novelApp = novelAppService.getByAppId(createdAppPay.getAppId());
            AppPayWithConfigDTO appPayWithConfigDTO = appPayService.getAppPayByAppId(createdAppPay.getAppId());
            CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequestForCreate(appPayWithConfigDTO, novelApp, request);
            localFileOperationService.createPayConfig(createNovelAppRequest, rollbackActions, request.getPayType());

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
    @OperationLog(opType = OpType.QUERY_CODE, opName = "根据appid获取所有支付类型的配置信息")
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
    @OperationLog(opType = OpType.UPDATE_CODE, opName = "更新小程序支付配置")
    public Result<AppPayWithConfigDTO> updateAppPay(
            @Parameter(description = "支付配置信息", required = true)
            @Valid @RequestBody UpdateAppPayRequest request) {
        List<Runnable> rollbackActions = new ArrayList<>();
        try {
            // 1. 数据库操作
            AppPayWithConfigDTO updatedAppPay = appPayService.updateAppPay(request);
            // 2. 文件操作
            NovelApp novelApp = novelAppService.getByAppId(updatedAppPay.getAppId());
            // 使用专门为更新单个支付配置设计的转换方法
            CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequestForUpdate(updatedAppPay, novelApp, request);
            localFileOperationService.createPayConfig(createNovelAppRequest,rollbackActions, request.getPayType());
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
    @OperationLog(opType = OpType.DELETE_CODE, opName = "根据appId和支付类型删除支付配置")
    public Result<String> deleteAppPayByAppIdAndType(
            @Parameter(description = "小程序ID", required = true)
            @RequestParam String appId,
            @Parameter(description = "支付类型", required = true)
            @RequestParam String payType) {
        List<Runnable> rollbackActions = new ArrayList<>();

        try {
            boolean success = appPayService.deleteAppPayByAppIdAndType(appId, payType);
            if (success) {
                NovelApp novelApp = novelAppService.getByAppId(appId);
                AppPayWithConfigDTO appPayByAppId = appPayService.getAppPayByAppId(appId);
                // 使用统一的转换方法，确保完整配置传递
                CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequest(appPayByAppId, novelApp);
                localFileOperationService.deletePayConfig(createNovelAppRequest, rollbackActions, payType);


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
        baseConfig.setDeliverId(novelApp.getDeliverId());
        baseConfig.setBannerId(novelApp.getBannerId());
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
            commonConfig.setReaderLoginType(dbCommonConfig.getReaderLoginType());
            commonConfig.setWeixinAppToken(dbCommonConfig.getWeixinAppToken());
            commonConfig.setDouyinAppToken(dbCommonConfig.getDouyinAppToken());
            commonConfig.setBaiduAppToken(dbCommonConfig.getBaiduAppToken());
            commonConfig.setIaaMode(dbCommonConfig.getIaaMode());
            commonConfig.setIaaDialogStyle(dbCommonConfig.getIaaDialogStyle());

        }
        req.setCommonConfig(commonConfig);

        // 注意：这里不自动填充所有支付配置项，只在特定场景下填充需要更新的配置项
        // 这样可以确保在更新单个支付配置时，其他配置项不会被重置
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
            // imPay
            if (appPayWithConfigDTO.getImPay() != null) {
                CreateNovelAppRequest.PayTypeConfig imPay = new CreateNovelAppRequest.PayTypeConfig();
                imPay.setEnabled(appPayWithConfigDTO.getImPay().getEnabled());
                imPay.setGatewayAndroid(appPayWithConfigDTO.getImPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getImPay().getGatewayAndroid().toString());
                imPay.setGatewayIos(appPayWithConfigDTO.getImPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getImPay().getGatewayIos().toString());
                paymentConfig.setImPay(imPay);
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
        // 其它配置如有需要可补充
        return req;
    }
    
    /**
     * 为创建单个支付配置专门创建的转换方法
     * @param appPayWithConfigDTO 应用支付配置
     * @param novelApp 应用信息
     * @param request 创建请求
     * @return CreateNovelAppRequest对象
     */
    private CreateNovelAppRequest convertToCreateNovelAppRequestForCreate(AppPayWithConfigDTO appPayWithConfigDTO, NovelApp novelApp, CreateAppPayRequest request) {
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
        baseConfig.setDeliverId(novelApp.getDeliverId());
        baseConfig.setBannerId(novelApp.getBannerId());
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
            commonConfig.setReaderLoginType(dbCommonConfig.getReaderLoginType());
            commonConfig.setWeixinAppToken(dbCommonConfig.getWeixinAppToken());
            commonConfig.setDouyinAppToken(dbCommonConfig.getDouyinAppToken());
            commonConfig.setBaiduAppToken(dbCommonConfig.getBaiduAppToken());
            commonConfig.setIaaMode(dbCommonConfig.getIaaMode());
            commonConfig.setIaaDialogStyle(dbCommonConfig.getIaaDialogStyle());

        }
        req.setCommonConfig(commonConfig);

        // 只设置需要创建的支付配置项
        CreateNovelAppRequest.PaymentConfig paymentConfig = new CreateNovelAppRequest.PaymentConfig();
        CreateNovelAppRequest.PayTypeConfig payTypeConfig = new CreateNovelAppRequest.PayTypeConfig();
        payTypeConfig.setEnabled(request.getEnabled());
        payTypeConfig.setGatewayAndroid(request.getGatewayAndroid() == null ? null : request.getGatewayAndroid().toString());
        payTypeConfig.setGatewayIos(request.getGatewayIos() == null ? null : request.getGatewayIos().toString());

        switch (request.getPayType()) {
            case "normalPay":
                paymentConfig.setNormalPay(payTypeConfig);
                break;
            case "orderPay":
                paymentConfig.setOrderPay(payTypeConfig);
                break;
            case "renewPay":
                paymentConfig.setRenewPay(payTypeConfig);
                break;
            case "douzuanPay":
                paymentConfig.setDouzuanPay(payTypeConfig);
                break;
            case "imPay":
                paymentConfig.setImPay(payTypeConfig);
                break;
            case "wxVirtualPay":
                paymentConfig.setWxVirtualPay(payTypeConfig);
                break;
            case "wxVirtualRenewPay":
                paymentConfig.setWxVirtualRenewPay(payTypeConfig);
                break;
        }
        req.setPaymentConfig(paymentConfig);

        // 其它配置如有需要可补充
        return req;
    }
    
    /**
     * 为更新单个支付配置专门创建的转换方法
     * @param updatedPayConfig 更新的支付配置
     * @param novelApp 应用信息
     * @param request 更新请求
     * @return CreateNovelAppRequest对象
     */
    private CreateNovelAppRequest convertToCreateNovelAppRequestForUpdate(AppPayWithConfigDTO updatedPayConfig, NovelApp novelApp, UpdateAppPayRequest request) {
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
        baseConfig.setDeliverId(novelApp.getDeliverId());
        baseConfig.setBannerId(novelApp.getBannerId());
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
            commonConfig.setReaderLoginType(dbCommonConfig.getReaderLoginType());
            commonConfig.setWeixinAppToken(dbCommonConfig.getWeixinAppToken());
            commonConfig.setDouyinAppToken(dbCommonConfig.getDouyinAppToken());
            commonConfig.setBaiduAppToken(dbCommonConfig.getBaiduAppToken());
            commonConfig.setIaaMode(dbCommonConfig.getIaaMode());
            commonConfig.setIaaDialogStyle(dbCommonConfig.getIaaDialogStyle());

        }
        req.setCommonConfig(commonConfig);

        // 只设置需要更新的支付配置项
        CreateNovelAppRequest.PaymentConfig paymentConfig = new CreateNovelAppRequest.PaymentConfig();
        CreateNovelAppRequest.PayTypeConfig payTypeConfig = new CreateNovelAppRequest.PayTypeConfig();
        payTypeConfig.setEnabled(request.getEnabled());
        payTypeConfig.setGatewayAndroid(request.getGatewayAndroid() == null ? null : request.getGatewayAndroid().toString());
        payTypeConfig.setGatewayIos(request.getGatewayIos() == null ? null : request.getGatewayIos().toString());
        
        switch (request.getPayType()) {
            case "normalPay":
                paymentConfig.setNormalPay(payTypeConfig);
                break;
            case "orderPay":
                paymentConfig.setOrderPay(payTypeConfig);
                break;
            case "renewPay":
                paymentConfig.setRenewPay(payTypeConfig);
                break;
            case "douzuanPay":
                paymentConfig.setDouzuanPay(payTypeConfig);
                break;
            case "imPay":
                paymentConfig.setImPay(payTypeConfig);
                break;
            case "wxVirtualPay":
                paymentConfig.setWxVirtualPay(payTypeConfig);
                break;
            case "wxVirtualRenewPay":
                paymentConfig.setWxVirtualRenewPay(payTypeConfig);
                break;
        }
        req.setPaymentConfig(paymentConfig);

        // 其它配置如有需要可补充
        return req;
    }
    
    /**
     * 为删除单个支付配置专门创建的转换方法
     * @param appPayWithConfigDTO 应用支付配置
     * @param novelApp 应用信息
     * @param payType 支付类型
     * @return CreateNovelAppRequest对象
     */
    private CreateNovelAppRequest convertToCreateNovelAppRequestForDelete(AppPayWithConfigDTO appPayWithConfigDTO, NovelApp novelApp, String payType) {
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
        baseConfig.setDeliverId(novelApp.getDeliverId());
        baseConfig.setBannerId(novelApp.getBannerId());
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
            commonConfig.setReaderLoginType(dbCommonConfig.getReaderLoginType());
            commonConfig.setWeixinAppToken(dbCommonConfig.getWeixinAppToken());
            commonConfig.setDouyinAppToken(dbCommonConfig.getDouyinAppToken());
            commonConfig.setBaiduAppToken(dbCommonConfig.getBaiduAppToken());
            commonConfig.setIaaMode(dbCommonConfig.getIaaMode());
            commonConfig.setIaaDialogStyle(dbCommonConfig.getIaaDialogStyle());
        }
        req.setCommonConfig(commonConfig);

        // 只设置需要删除的支付配置项
        CreateNovelAppRequest.PaymentConfig paymentConfig = new CreateNovelAppRequest.PaymentConfig();
        if (appPayWithConfigDTO != null) {
            // normalPay
            if (appPayWithConfigDTO.getNormalPay() != null) {
                CreateNovelAppRequest.PayTypeConfig normalPay = new CreateNovelAppRequest.PayTypeConfig();
                normalPay.setEnabled(appPayWithConfigDTO.getNormalPay().getEnabled());
                normalPay.setGatewayAndroid(appPayWithConfigDTO.getNormalPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getNormalPay().getGatewayAndroid().toString());
                normalPay.setGatewayIos(appPayWithConfigDTO.getNormalPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getNormalPay().getGatewayIos().toString());
                
                // 如果是删除的支付类型，则设置为禁用
                if ("normalPay".equals(payType)) {
                    normalPay.setEnabled(false);
                }
                paymentConfig.setNormalPay(normalPay);
            }
            // orderPay
            if (appPayWithConfigDTO.getOrderPay() != null) {
                CreateNovelAppRequest.PayTypeConfig orderPay = new CreateNovelAppRequest.PayTypeConfig();
                orderPay.setEnabled(appPayWithConfigDTO.getOrderPay().getEnabled());
                orderPay.setGatewayAndroid(appPayWithConfigDTO.getOrderPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getOrderPay().getGatewayAndroid().toString());
                orderPay.setGatewayIos(appPayWithConfigDTO.getOrderPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getOrderPay().getGatewayIos().toString());
                
                // 如果是删除的支付类型，则设置为禁用
                if ("orderPay".equals(payType)) {
                    orderPay.setEnabled(false);
                }
                paymentConfig.setOrderPay(orderPay);
            }
            // renewPay
            if (appPayWithConfigDTO.getRenewPay() != null) {
                CreateNovelAppRequest.PayTypeConfig renewPay = new CreateNovelAppRequest.PayTypeConfig();
                renewPay.setEnabled(appPayWithConfigDTO.getRenewPay().getEnabled());
                renewPay.setGatewayAndroid(appPayWithConfigDTO.getRenewPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getRenewPay().getGatewayAndroid().toString());
                renewPay.setGatewayIos(appPayWithConfigDTO.getRenewPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getRenewPay().getGatewayIos().toString());
                
                // 如果是删除的支付类型，则设置为禁用
                if ("renewPay".equals(payType)) {
                    renewPay.setEnabled(false);
                }
                paymentConfig.setRenewPay(renewPay);
            }
            // douzuanPay
            if (appPayWithConfigDTO.getDouzuanPay() != null) {
                CreateNovelAppRequest.PayTypeConfig douzuanPay = new CreateNovelAppRequest.PayTypeConfig();
                douzuanPay.setEnabled(appPayWithConfigDTO.getDouzuanPay().getEnabled());
                douzuanPay.setGatewayAndroid(appPayWithConfigDTO.getDouzuanPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getDouzuanPay().getGatewayAndroid().toString());
                douzuanPay.setGatewayIos(appPayWithConfigDTO.getDouzuanPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getDouzuanPay().getGatewayIos().toString());
                
                // 如果是删除的支付类型，则设置为禁用
                if ("douzuanPay".equals(payType)) {
                    douzuanPay.setEnabled(false);
                }
                paymentConfig.setDouzuanPay(douzuanPay);
            }

            // imPay
            if (appPayWithConfigDTO.getImPay() != null) {
                CreateNovelAppRequest.PayTypeConfig imPay = new CreateNovelAppRequest.PayTypeConfig();
                imPay.setEnabled(appPayWithConfigDTO.getImPay().getEnabled());
                imPay.setGatewayAndroid(appPayWithConfigDTO.getImPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getImPay().getGatewayAndroid().toString());
                imPay.setGatewayIos(appPayWithConfigDTO.getImPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getImPay().getGatewayIos().toString());

                // 如果是删除的支付类型，则设置为禁用
                if ("imPay".equals(payType)) {
                    imPay.setEnabled(false);
                }
                paymentConfig.setImPay(imPay);
            }
            // wxVirtualPay
            if (appPayWithConfigDTO.getWxVirtualPay() != null) {
                CreateNovelAppRequest.PayTypeConfig wxVirtualPay = new CreateNovelAppRequest.PayTypeConfig();
                wxVirtualPay.setEnabled(appPayWithConfigDTO.getWxVirtualPay().getEnabled());
                wxVirtualPay.setGatewayAndroid(appPayWithConfigDTO.getWxVirtualPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getWxVirtualPay().getGatewayAndroid().toString());
                wxVirtualPay.setGatewayIos(appPayWithConfigDTO.getWxVirtualPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getWxVirtualPay().getGatewayIos().toString());
                
                // 如果是删除的支付类型，则设置为禁用
                if ("wxVirtualPay".equals(payType)) {
                    wxVirtualPay.setEnabled(false);
                }
                paymentConfig.setWxVirtualPay(wxVirtualPay);
            }
            // wxVirtualPay
            if (appPayWithConfigDTO.getWxVirtualRenewPay() != null) {
                CreateNovelAppRequest.PayTypeConfig wxVirtualRenewPay = new CreateNovelAppRequest.PayTypeConfig();
                wxVirtualRenewPay.setEnabled(appPayWithConfigDTO.getWxVirtualRenewPay().getEnabled());
                wxVirtualRenewPay.setGatewayAndroid(appPayWithConfigDTO.getWxVirtualRenewPay().getGatewayAndroid() == null ? null : appPayWithConfigDTO.getWxVirtualRenewPay().getGatewayAndroid().toString());
                wxVirtualRenewPay.setGatewayIos(appPayWithConfigDTO.getWxVirtualRenewPay().getGatewayIos() == null ? null : appPayWithConfigDTO.getWxVirtualRenewPay().getGatewayIos().toString());

                // 如果是删除的支付类型，则设置为禁用
                if ("wxVirtualRenewPay".equals(payType)) {
                    wxVirtualRenewPay.setEnabled(false);
                }
                paymentConfig.setWxVirtualRenewPay(wxVirtualRenewPay);
            }
        }
        req.setPaymentConfig(paymentConfig);

        // 其它配置如有需要可补充
        return req;
    }
}
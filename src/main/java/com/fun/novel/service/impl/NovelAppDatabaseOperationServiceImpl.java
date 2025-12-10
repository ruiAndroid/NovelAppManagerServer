package com.fun.novel.service.impl;

import com.fun.novel.dto.*;
import com.fun.novel.entity.*;
import com.fun.novel.service.*;
import com.fun.novel.utils.CreateNovelTaskLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NovelAppDatabaseOperationServiceImpl implements NovelAppDatabaseOperationService {
    @Autowired
    private NovelAppService novelAppService;
    @Autowired
    private AppPayService appPayService;
    @Autowired
    private AppAdService appAdService;
    @Autowired
    private AdConfigService adConfigService;
    @Autowired
    private AppCommonConfigService appCommonConfigService;
    @Autowired
    private AppUIConfigService uiConfigService;
    @Autowired
    private CreateNovelTaskLogger taskLogger;

    private static final int DB_STEP_DELAY_MS = 500;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processDatabaseOperations(String taskId, CreateNovelAppRequest params) {
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();
        CreateNovelAppRequest.AdConfig adConfig = params.getAdConfig();
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.UiConfig uiConfig = params.getUiConfig();
        String appId = baseConfig.getAppid();
        //1.1 记录novel_app表
        taskLogger.log(taskId, "[1-1] 开始更新novel_app表...", CreateNovelLogType.PROCESSING);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        taskLogger.log(taskId, "[1-1-1] 插入novel_app表基础信息", CreateNovelLogType.INFO);
        NovelApp novelApp = new NovelApp();
        novelApp.setAppName(baseConfig.getAppName());
        novelApp.setAppCode(baseConfig.getAppCode());
        novelApp.setPlatform(baseConfig.getPlatform());
        novelApp.setVersion(baseConfig.getVersion());
        novelApp.setProduct(baseConfig.getProduct());
        novelApp.setCustomer(baseConfig.getCustomer());
        novelApp.setAppid(baseConfig.getAppid());
        novelApp.setTokenId(baseConfig.getTokenId());
        novelApp.setCl(baseConfig.getCl());
        novelApp.setDeliverId(baseConfig.getDeliverId());
        novelApp.setBannerId(baseConfig.getBannerId());
        taskLogger.log(taskId, "[1-1-2] 插入novel_app表theme信息", CreateNovelLogType.INFO);
        taskLogger.log(taskId, "[1-1-3] 插入novel_app表deliver信息", CreateNovelLogType.INFO);
        NovelApp existingApp = novelAppService.getByAppId(appId);
        if (existingApp != null) {
            taskLogger.log(taskId, "[1-1] 小说应用已存在，创建失败", CreateNovelLogType.ERROR);
            throw new RuntimeException("小说应用已存在，创建失败");
        }
        NovelApp createdApp = novelAppService.addNovelApp(novelApp);
        taskLogger.log(taskId, "[1-1] novel_app表更新成功", CreateNovelLogType.SUCCESS);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        //1-2 记录app_pay表
        taskLogger.log(taskId, "[1-2] 开始更新app_pay表...", CreateNovelLogType.PROCESSING);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        Map<String, CreateNovelAppRequest.PayTypeConfig> payTypeMap = new LinkedHashMap<>();
        payTypeMap.put("normalPay", paymentConfig.getNormalPay());
        payTypeMap.put("orderPay", paymentConfig.getOrderPay());
        payTypeMap.put("douzuanPay", paymentConfig.getDouzuanPay());
        payTypeMap.put("imPay", paymentConfig.getImPay());
        payTypeMap.put("renewPay", paymentConfig.getRenewPay());
        payTypeMap.put("wxVirtualPay", paymentConfig.getWxVirtualPay());
        payTypeMap.put("wxVirtualRenewPay", paymentConfig.getWxVirtualRenewPay());

        for (Map.Entry<String, CreateNovelAppRequest.PayTypeConfig> entry : payTypeMap.entrySet()) {
            String payType = entry.getKey();
            CreateNovelAppRequest.PayTypeConfig payConfig = entry.getValue();
            if (payConfig == null) continue;
            taskLogger.log(taskId, "[1-2] 检查支付类型:" + payType, CreateNovelLogType.PROCESSING);
            AppPayWithConfigDTO appPayConfigDTO = appPayService.getAppPayByAppId(appId);
            boolean exists = false;
            if (appPayConfigDTO != null) {
                switch (payType) {
                    case "normalPay": exists = appPayConfigDTO.getNormalPay() != null; break;
                    case "orderPay": exists = appPayConfigDTO.getOrderPay() != null; break;
                    case "douzuanPay": exists = appPayConfigDTO.getDouzuanPay() != null; break;
                    case "imPay": exists = appPayConfigDTO.getImPay() != null; break;
                    case "renewPay": exists = appPayConfigDTO.getRenewPay() != null; break;
                    case "wxVirtualPay": exists = appPayConfigDTO.getWxVirtualPay() != null; break;
                    case "wxVirtualRenewPay": exists = appPayConfigDTO.getWxVirtualRenewPay() != null; break;
                }
            }
            if (!exists) {
                if (!Boolean.TRUE.equals(payConfig.getEnabled())) {
                    continue;
                }
                taskLogger.log(taskId, "[1-2] 新增支付类型:" + payType, CreateNovelLogType.PROCESSING);
                CreateAppPayRequest payRequest = new CreateAppPayRequest();
                payRequest.setAppId(appId);
                payRequest.setPayType(payType);
                payRequest.setEnabled(true);
                payRequest.setGatewayAndroid(payConfig.getGatewayAndroid() == null || payConfig.getGatewayAndroid().isEmpty() ? 0 : Integer.parseInt(payConfig.getGatewayAndroid()));
                payRequest.setGatewayIos(payConfig.getGatewayIos() == null || payConfig.getGatewayIos().isEmpty() ? 0 : Integer.parseInt(payConfig.getGatewayIos()));
                appPayService.createAppPay(payRequest);
                taskLogger.log(taskId, "[1-2] 新增支付类型: " + payType + " 成功", CreateNovelLogType.SUCCESS);
            } else {
                taskLogger.log(taskId, "[1-2] 更新支付类型:" + payType, CreateNovelLogType.PROCESSING);
                UpdateAppPayRequest payRequest = new UpdateAppPayRequest();
                payRequest.setAppId(appId);
                payRequest.setPayType(payType);
                payRequest.setEnabled(Boolean.TRUE.equals(payConfig.getEnabled()));
                payRequest.setGatewayAndroid(payConfig.getGatewayAndroid() == null || payConfig.getGatewayAndroid().isEmpty() ? 0 : Integer.parseInt(payConfig.getGatewayAndroid()));
                payRequest.setGatewayIos(payConfig.getGatewayIos() == null || payConfig.getGatewayIos().isEmpty() ? 0 : Integer.parseInt(payConfig.getGatewayIos()));
                appPayService.updateAppPay(payRequest);
                taskLogger.log(taskId, "[1-2] 更新支付类型: " + payType + " 成功", CreateNovelLogType.SUCCESS);
            }
            try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        taskLogger.log(taskId, "[1-2] app_pay表更新成功", CreateNovelLogType.SUCCESS);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        //1-3 记录app_ad表
        taskLogger.log(taskId, "[1-3] 开始更新app_ad表...", CreateNovelLogType.PROCESSING);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        AppAd appAd = new AppAd();
        appAd.setAppid(appId);
        AppAd createdAppAd = appAdService.addAppAd(appAd);
        taskLogger.log(taskId, "[1-3-1] app_ad表写入成功", CreateNovelLogType.SUCCESS);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        // 1-3-2 记录ad_config表
        taskLogger.log(taskId, "[1-3-2] 开始写入ad_config表...", CreateNovelLogType.PROCESSING);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        Map<String, Object> adTypeMap = new LinkedHashMap<>();
        adTypeMap.put("reward", adConfig.getRewardAd());
        adTypeMap.put("interstitial", adConfig.getInterstitialAd());
        for (Map.Entry<String, Object> entry : adTypeMap.entrySet()) {
            String adType = entry.getKey();
            Object adConfigObj = entry.getValue();
            if (adConfigObj == null) continue;
            AdConfig adConfigEntity = new AdConfig();
            adConfigEntity.setAppAdId(createdAppAd.getId());
            adConfigEntity.setAdType(adType);
            boolean enabled = false;
            switch (adType) {
                case "reward": {
                    CreateNovelAppRequest.RewardAdConfig reward = (CreateNovelAppRequest.RewardAdConfig) adConfigObj;
                    enabled = Boolean.TRUE.equals(reward.getEnabled());
                    if (!enabled) continue;
                    adConfigEntity.setIsRewardAdEnabled(true);
                    adConfigEntity.setRewardAdId(reward.getRewardAdId());
                    adConfigEntity.setRewardCount(reward.getRewardCount());
                    break;
                }
                case "interstitial": {
                    CreateNovelAppRequest.InterstitialAdConfig interstitial = (CreateNovelAppRequest.InterstitialAdConfig) adConfigObj;
                    enabled = Boolean.TRUE.equals(interstitial.getEnabled());
                    if (!enabled) continue;
                    adConfigEntity.setIsInterstitialAdEnabled(true);
                    adConfigEntity.setInterstitialAdId(interstitial.getInterstitialAdId());
                    adConfigEntity.setInterstitialCount(interstitial.getInterstitialCount());
                    break;
                }
                case "banner": {
                    CreateNovelAppRequest.BannerAdConfig banner = (CreateNovelAppRequest.BannerAdConfig) adConfigObj;
                    enabled = Boolean.TRUE.equals(banner.getEnabled());
                    if (!enabled) continue;
                    adConfigEntity.setIsBannerAdEnabled(true);
                    adConfigEntity.setBannerAdId(banner.getBannerAdId());
                    break;
                }
            }
            adConfigService.addAdConfig(adConfigEntity);
            taskLogger.log(taskId, "[1-3-2] " + adType + " 广告配置写入成功", CreateNovelLogType.SUCCESS);
            try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        taskLogger.log(taskId, "[1-3-2] ad_config表写入成功", CreateNovelLogType.SUCCESS);
        taskLogger.log(taskId, "[1-3] app_ad表更新成功", CreateNovelLogType.SUCCESS);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        //1.4 记录通用配置 app_common_config表
        taskLogger.log(taskId, "[1-4] 开始更新app_common_config表...", CreateNovelLogType.PROCESSING);
        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        AppCommonConfigDTO commonConfigDTO = new AppCommonConfigDTO();
        commonConfigDTO.setAppId(appId);
        commonConfigDTO.setContact(commonConfig.getContact());
        commonConfigDTO.setDouyinImId(commonConfig.getDouyinImId());
        commonConfigDTO.setKuaishouClientId(commonConfig.getKuaishouClientId());
        commonConfigDTO.setKuaishouClientSecret(commonConfig.getKuaishouClientSecret());
        commonConfigDTO.setKuaishouAppToken(commonConfig.getKuaishouAppToken());
        commonConfigDTO.setWeixinAppToken(commonConfig.getWeixinAppToken());
        commonConfigDTO.setBaiduAppToken(commonConfig.getBaiduAppToken());
        commonConfigDTO.setBuildCode(commonConfig.getBuildCode());
        commonConfigDTO.setDouyinAppToken(commonConfig.getDouyinAppToken());
        commonConfigDTO.setIaaMode(commonConfig.getIaaMode());
        commonConfigDTO.setIaaDialogStyle(commonConfig.getIaaDialogStyle());

        commonConfigDTO.setReaderLoginType(commonConfig.getReaderLoginType());
        commonConfigDTO.setMineLoginType(commonConfig.getMineLoginType());
        AppCommonConfig existingCommonConfig = appCommonConfigService.getAppCommonConfig(appId);
        if (existingCommonConfig == null) {
            taskLogger.log(taskId, "[1-4] 新增通用配置...", CreateNovelLogType.PROCESSING);
            appCommonConfigService.createAppCommonConfig(commonConfigDTO);
            taskLogger.log(taskId, "[1-4] 新增通用配置成功", CreateNovelLogType.SUCCESS);
        } else {
            taskLogger.log(taskId, "[1-4] 更新通用配置...", CreateNovelLogType.PROCESSING);
            appCommonConfigService.updateAppCommonConfig(commonConfigDTO);
            taskLogger.log(taskId, "[1-4] 更新通用配置成功", CreateNovelLogType.SUCCESS);
        }
        taskLogger.log(taskId, "[1-4] app_common_config表更新成功", CreateNovelLogType.SUCCESS);
        //1.5 记录UI配置 app_ui_config表
        taskLogger.log(taskId, "[1-5] 开始更新app_ui_config表...", CreateNovelLogType.PROCESSING);

        AppUIConfig appUIConfig = new AppUIConfig();
        appUIConfig.setAppId(appId);
        appUIConfig.setMainTheme(uiConfig.getMainTheme());
        appUIConfig.setSecondTheme(uiConfig.getSecondTheme());
        appUIConfig.setPayCardStyle(uiConfig.getPayCardStyle());
        appUIConfig.setHomeCardStyle(uiConfig.getHomeCardStyle());
        AppUIConfig existingUiConfig = uiConfigService.getByAppId(appId);
        if (existingUiConfig == null){
            taskLogger.log(taskId, "[1-5] 新增UI配置...", CreateNovelLogType.PROCESSING);
            uiConfigService.createAppUIConfig(appUIConfig);
            taskLogger.log(taskId, "[1-5] 新增UI配置成功", CreateNovelLogType.SUCCESS);
        }else{
            taskLogger.log(taskId, "[1-5] 更新UI配置...", CreateNovelLogType.PROCESSING);
            uiConfigService.updateAppUIConfig(appUIConfig);
            taskLogger.log(taskId, "[1-5] 更新UI配置成功", CreateNovelLogType.SUCCESS);
        }



        try { Thread.sleep(DB_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
} 
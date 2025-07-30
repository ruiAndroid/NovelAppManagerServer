package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.entity.AppAd;
import com.fun.novel.entity.AdConfig;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.service.*;
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

    @Autowired
    private NovelAppLocalFileOperationService novelAppLocalFileOperationService;

    @Autowired
    private NovelAppService novelAppService;

    @Autowired
    private AppCommonConfigService appCommonConfigService;

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
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();
        try {
            // 1. 数据库操作
            AdConfig createdAdConfig = adConfigService.addAdConfig(adConfig);
            String appid = appAdService.getAppIdByAppAdId(createdAdConfig.getAppAdId());
            NovelApp novelApp = novelAppService.getByAppId(appid);
            AppAdWithConfigDTO appAdWithConfig = appAdService.getAppAdByAppId(appid);

            // 2. 文件操作
            CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequest(appAdWithConfig,novelApp);
            novelAppLocalFileOperationService.updateAdConfigLocalCodeFiles(createNovelAppRequest, rollbackActions);
            return Result.success("创建成功", createdAdConfig);
        } catch (Exception e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error("创建失败: " + e.getMessage());
        }
    }


    @PostMapping("/adConfig/update")
    @Operation(summary = "更新AdConfig", description = "更新广告配置信息")
    public Result<AdConfig> updateAdConfig(
            @Parameter(description = "更新广告配置请求", required = true)
            @Valid @RequestBody UpdateAdConfigRequest request) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();
        try {
            // 1. 数据库操作
            AdConfig updatedAdConfig = adConfigService.updateAdConfig(request);
            String appid = appAdService.getAppIdByAppAdId(updatedAdConfig.getAppAdId());
            NovelApp novelApp = novelAppService.getByAppId(appid);
            AppAdWithConfigDTO appAdWithConfig = appAdService.getAppAdByAppId(appid);

            
            // 2. 文件操作
            CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequest(appAdWithConfig,novelApp);
            novelAppLocalFileOperationService.updateAdConfigLocalCodeFiles(createNovelAppRequest, rollbackActions);
            return Result.success("更新成功", updatedAdConfig);
        } catch (Exception e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/adConfig/deleteByAppAdIdAndType")
    @Operation(summary = "删除AdConfig", description = "根据appAdId和广告类型删除广告配置")
    public Result<String> deleteAdConfigByAppAdIdAndType(
            @Parameter(description = "应用广告ID", required = true)
            @RequestParam Integer appAdId,
            @Parameter(description = "广告类型", required = true)
            @RequestParam String adType) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();

        try {
            boolean success = adConfigService.deleteAdConfigByAppAdIdAndType(appAdId, adType);
            if (success) {

                String appid = appAdService.getAppIdByAppAdId(appAdId);
                NovelApp novelApp = novelAppService.getByAppId(appid);
                AppAdWithConfigDTO appAdWithConfig = appAdService.getAppAdByAppId(appid);
                CreateNovelAppRequest createNovelAppRequest = convertToCreateNovelAppRequest(appAdWithConfig,novelApp);
                novelAppLocalFileOperationService.updateAdConfigLocalCodeFiles(createNovelAppRequest, rollbackActions);
                return Result.success("删除成功");
            } else {
                return Result.error("未找到对应的广告配置记录");
            }
        } catch (IllegalArgumentException e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error(e.getMessage());
        }
    }

    private CreateNovelAppRequest convertToCreateNovelAppRequest(AppAdWithConfigDTO appAdWithConfig,NovelApp novelApp) {
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

       
        CreateNovelAppRequest.AdConfig adConfig = new CreateNovelAppRequest.AdConfig();
        if (appAdWithConfig != null) {
            // reward广告
            if (appAdWithConfig.getReward() != null) {
                CreateNovelAppRequest.RewardAdConfig rewardAdConfig = new CreateNovelAppRequest.RewardAdConfig();
                rewardAdConfig.setEnabled(appAdWithConfig.getReward().getIsRewardAdEnabled());
                rewardAdConfig.setRewardAdId(appAdWithConfig.getReward().getRewardAdId());
                rewardAdConfig.setRewardCount(appAdWithConfig.getReward().getRewardCount());
                adConfig.setRewardAd(rewardAdConfig);
            }
            // interstitial广告
            if (appAdWithConfig.getInterstitial() != null) {
                CreateNovelAppRequest.InterstitialAdConfig interstitialAdConfig = new CreateNovelAppRequest.InterstitialAdConfig();
                interstitialAdConfig.setEnabled(appAdWithConfig.getInterstitial().getIsInterstitialAdEnabled());
                interstitialAdConfig.setInterstitialAdId(appAdWithConfig.getInterstitial().getInterstitialAdId());
                interstitialAdConfig.setInterstitialCount(appAdWithConfig.getInterstitial().getInterstitialCount());
                adConfig.setInterstitialAd(interstitialAdConfig);
            }
            // banner广告
            if (appAdWithConfig.getBanner() != null) {
                CreateNovelAppRequest.BannerAdConfig bannerAdConfig = new CreateNovelAppRequest.BannerAdConfig();
                bannerAdConfig.setEnabled(appAdWithConfig.getBanner().getIsBannerAdEnabled());
                bannerAdConfig.setBannerAdId(appAdWithConfig.getBanner().getBannerAdId());
                adConfig.setBannerAd(bannerAdConfig);
            }
        }
        req.setAdConfig(adConfig);
        CreateNovelAppRequest.DeliverConfig deliverConfig = new CreateNovelAppRequest.DeliverConfig();
        deliverConfig.setDeliverId(novelApp.getDeliverId());
        deliverConfig.setBannerId(novelApp.getBannerId());
        req.setDeliverConfig(deliverConfig);
        // 其它配置如有需要可补充
        return req;
    }

} 
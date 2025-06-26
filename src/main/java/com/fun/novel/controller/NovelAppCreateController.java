package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.dto.CreateAppPayRequest;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.entity.AdConfig;
import com.fun.novel.entity.AppAd;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.service.AdConfigService;
import com.fun.novel.service.AppAdService;
import com.fun.novel.service.AppPayService;
import com.fun.novel.service.NovelAppService;
import com.fun.novel.utils.CreateNovelTaskManager;
import com.fun.novel.dto.CreateNovelLogMessage;
import com.fun.novel.dto.CreateNovelLogType;
import com.fun.novel.dto.UpdateAppPayRequest;
import com.fun.novel.dto.AppPayWithConfigDTO;
import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.service.AppCommonConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/novel-create")
@Tag(name = "小程序发布", description = "小程序创建相关接口")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://172.17.5.80:5173",
    "http://172.17.5.80:8080"
}, allowCredentials = "true")
public class NovelAppCreateController {
    private static final Logger logger = LoggerFactory.getLogger(NovelAppCreateController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CreateNovelTaskManager createNovelTaskManager;

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

    private static final Map<String, String> PLATFORM_NAMES = new HashMap<>();
    static {
        PLATFORM_NAMES.put("mp-toutiao", "抖音小程序");
        PLATFORM_NAMES.put("mp-weixin", "微信小程序");
        PLATFORM_NAMES.put("mp-kuaishou", "快手小程序");
        PLATFORM_NAMES.put("mp-baidu", "百度小程序");
    }

    /**
     * 实时推送日志到前端
     * @param message 日志内容
     */
    public void sendLogToClient(String taskId, String message, CreateNovelLogType type) {
        String destination = "/topic/novel-create-log/" + taskId;
        messagingTemplate.convertAndSend(destination, CreateNovelLogMessage.from(message, type));
        // 控制台打印
        logger.info("[NovelCreateLog] taskId={} message={}", taskId, message);
    }


    @Operation(summary = "创建小说小程序")
    @PostMapping("/createNovelApp")
    public Result<Map<String, String>> createNovelApp(@RequestBody CreateNovelAppRequest params) {
        String taskId = createNovelTaskManager.createTask();
        if (taskId == null) {
            return Result.error("已有小说创建任务正在进行中，请稍后再试");
        }
        // 立即返回taskId给前端
        Map<String, String> data = new HashMap<>();
        data.put("taskId", taskId);
        Result<Map<String, String>> result = Result.success("任务已启动", data);

        // 异步执行实际创建流程
        CompletableFuture.runAsync(() -> {
            String currentTaskId = taskId; // Make a final copy for the lambda

            List<Runnable> rollbackActions = new ArrayList<>();

            try {
                // 等待一段时间，给前端足够的时间来订阅
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Task thread interrupted", e);
                    sendLogToClient(currentTaskId, "任务线程被中断，创建失败", CreateNovelLogType.ERROR);
                    return;
                }
                sendLogToClient(currentTaskId, "参数校验已通过，开始执行创建小说小程序任务 taskId:" + currentTaskId, CreateNovelLogType.INFO);


                CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
                CreateNovelAppRequest.DeliverConfig deliverConfig = params.getDeliverConfig();
                CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();
                CreateNovelAppRequest.AdConfig adConfig = params.getAdConfig();
                CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();

                //1.数据库处理

                //1.1 记录novel_app表
                sendLogToClient(currentTaskId, "[1-1] 开始更新novel_app表...", CreateNovelLogType.PROCESSING);
                sendLogToClient(currentTaskId, "[1-1-1] 插入novel_app表基础信息", CreateNovelLogType.INFO);
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
                sendLogToClient(currentTaskId, "[1-1-2] 插入novel_app表theme信息", CreateNovelLogType.INFO);
                novelApp.setMainTheme(baseConfig.getMainTheme());
                novelApp.setSecondTheme(baseConfig.getSecondTheme());
                sendLogToClient(currentTaskId, "[1-1-3] 插入novel_app表deliver信息", CreateNovelLogType.INFO);
                novelApp.setDeliverId(deliverConfig.getDeliverId());
                novelApp.setBannerId(deliverConfig.getBannerId());
                // 主题、bannerId、deliverId等可后续补充
                NovelApp createdApp = novelAppService.addNovelApp(novelApp);
                rollbackActions.add(() -> {
                    try {
                        // 回滚动作：删除刚插入的记录
                        // 这里假设 novelAppService 有 deleteByAppId 方法
                        novelAppService.deleteByAppId(baseConfig.getAppid());
                    } catch (Exception ignore) {}
                });
                sendLogToClient(currentTaskId, "[1-1] novel_app表更新成功", CreateNovelLogType.SUCCESS);


                //1-2 记录app_pay表
                sendLogToClient(currentTaskId, "[1-2] 开始更新app_pay表...", CreateNovelLogType.PROCESSING);
                String appId = baseConfig.getAppid();
                Map<String, CreateNovelAppRequest.PayTypeConfig> payTypeMap = new LinkedHashMap<>();
                payTypeMap.put("normalPay", paymentConfig.getNormalPay());
                payTypeMap.put("orderPay", paymentConfig.getOrderPay());
                payTypeMap.put("douzuanPay", paymentConfig.getDouzuanPay());
                payTypeMap.put("renewPay", paymentConfig.getRenewPay());
                payTypeMap.put("wxVirtualPay", paymentConfig.getWxVirtualPay());

                for (Map.Entry<String, CreateNovelAppRequest.PayTypeConfig> entry : payTypeMap.entrySet()) {
                    String payType = entry.getKey();
                    CreateNovelAppRequest.PayTypeConfig payConfig = entry.getValue();
                    if (payConfig == null) continue;

                    sendLogToClient(currentTaskId, "[1-2] 检查支付类型:" + payType, CreateNovelLogType.PROCESSING);

                    // 查询当前appId下所有支付配置
                    AppPayWithConfigDTO appPayConfigDTO = appPayService.getAppPayByAppId(appId);
                    boolean exists = false;
                    if (appPayConfigDTO != null) {
                        switch (payType) {
                            case "normalPay": exists = appPayConfigDTO.getNormalPay() != null; break;
                            case "orderPay": exists = appPayConfigDTO.getOrderPay() != null; break;
                            case "douzuanPay": exists = appPayConfigDTO.getDouzuanPay() != null; break;
                            case "renewPay": exists = appPayConfigDTO.getRenewPay() != null; break;
                            case "wxVirtualPay": exists = appPayConfigDTO.getWxVirtualPay() != null; break;
                        }
                    }

                    if (!exists) {
                        // 如果支付类型未启用，则不插入新记录
                        if (!Boolean.TRUE.equals(payConfig.getEnabled())) {
                            continue;
                        }
                        sendLogToClient(currentTaskId, "[1-2] 新增支付类型:" + payType, CreateNovelLogType.PROCESSING);
                        CreateAppPayRequest payRequest = new CreateAppPayRequest();
                        payRequest.setAppId(appId);
                        payRequest.setPayType(payType);
                        payRequest.setEnabled(Boolean.TRUE.equals(payConfig.getEnabled()));
                        payRequest.setGatewayAndroid(payConfig.getGatewayAndroid() == null || payConfig.getGatewayAndroid().isEmpty() ? 0 : Integer.parseInt(payConfig.getGatewayAndroid()));
                        payRequest.setGatewayIos(payConfig.getGatewayIos() == null || payConfig.getGatewayIos().isEmpty() ? 0 : Integer.parseInt(payConfig.getGatewayIos()));
                        appPayService.createAppPay(payRequest);
                        sendLogToClient(currentTaskId, "[1-2] 新增支付类型: " + payType + " 成功", CreateNovelLogType.SUCCESS);
                        rollbackActions.add(() -> {
                            try { appPayService.deleteAppPayByAppIdAndType(appId, payType); } catch (Exception ignore) {}
                        });
                    } else {
                        sendLogToClient(currentTaskId, "[1-2] 更新支付类型:" + payType, CreateNovelLogType.PROCESSING);
                        UpdateAppPayRequest payRequest = new UpdateAppPayRequest();
                        payRequest.setAppId(appId);
                        payRequest.setPayType(payType);
                        payRequest.setEnabled(Boolean.TRUE.equals(payConfig.getEnabled()));
                        payRequest.setGatewayAndroid(payConfig.getGatewayAndroid() == null || payConfig.getGatewayAndroid().isEmpty() ? 0 : Integer.parseInt(payConfig.getGatewayAndroid()));
                        payRequest.setGatewayIos(payConfig.getGatewayIos() == null || payConfig.getGatewayIos().isEmpty() ? 0 : Integer.parseInt(payConfig.getGatewayIos()));
                        appPayService.updateAppPay(payRequest);
                        sendLogToClient(currentTaskId, "[1-2] 更新支付类型: " + payType + " 成功", CreateNovelLogType.SUCCESS);
                        // 可选：记录原值用于回滚
                    }
                }
                sendLogToClient(currentTaskId, "[1-2] app_pay表更新成功", CreateNovelLogType.SUCCESS);


                //1-3 记录app_ad表
                sendLogToClient(currentTaskId, "[1-3] 开始更新app_ad表...", CreateNovelLogType.PROCESSING);
                AppAd appAd = new AppAd();
                appAd.setAppid(appId);
                // 你可以根据 adConfig 设置广告主表的其他字段
                AppAd createdAppAd = appAdService.addAppAd(appAd);
                rollbackActions.add(() -> {
                    try { appAdService.deleteAppAdByAppId(appId); } catch (Exception ignore) {}
                });
                sendLogToClient(currentTaskId, "[1-3-1] app_ad表写入成功", CreateNovelLogType.SUCCESS);

                // 1-3-2 记录ad_config表
                sendLogToClient(currentTaskId, "[1-3-2] 开始写入ad_config表...", CreateNovelLogType.PROCESSING);

                Map<String, Object> adTypeMap = new LinkedHashMap<>();
                adTypeMap.put("reward", adConfig.getRewardAd());
                adTypeMap.put("interstitial", adConfig.getInterstitialAd());
//                adTypeMap.put("native", adConfig.getNativeAd());

                for (Map.Entry<String, Object> entry : adTypeMap.entrySet()) {
                    String adType = entry.getKey();
                    Object adConfigObj = entry.getValue();
                    if (adConfigObj == null) continue;

                    boolean enabled = false;
                    AdConfig adConfigEntity = new AdConfig();
                    adConfigEntity.setAppAdId(createdAppAd.getId());
                    adConfigEntity.setAdType(adType);

                    // 根据类型设置不同字段
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
                        // case "native": ...
                    }

                    adConfigService.addAdConfig(adConfigEntity);
                    sendLogToClient(currentTaskId, "[1-3-2] " + adType + " 广告配置写入成功", CreateNovelLogType.SUCCESS);

                    rollbackActions.add(() -> {
                        try { adConfigService.deleteAdConfigByAppAdIdAndType(createdAppAd.getId(), adType); } catch (Exception ignore) {}
                    });
                }
                sendLogToClient(currentTaskId, "[1-3-2] ad_config表写入成功", CreateNovelLogType.SUCCESS);

                sendLogToClient(currentTaskId, "[1-3] app_ad表更新成功", CreateNovelLogType.SUCCESS);

                //1.4 记录通用配置 app_common_config表
                sendLogToClient(currentTaskId, "[1-4] 开始更新app_common_config表...", CreateNovelLogType.PROCESSING);
                AppCommonConfigDTO commonConfigDTO = new AppCommonConfigDTO();
                commonConfigDTO.setAppId(appId);
                commonConfigDTO.setContact(commonConfig.getContact());
                commonConfigDTO.setDouyinImId(commonConfig.getDouyinImId());
                commonConfigDTO.setKuaishouClientId(commonConfig.getKuaishouClientId());
                commonConfigDTO.setKuaishouClientSecret(commonConfig.getKuaishouClientSecret());
                commonConfigDTO.setKuaishouAppToken(commonConfig.getKuaishouAppToken());
                commonConfigDTO.setWeixinAppToken(commonConfig.getWeixinAppToken());
                commonConfigDTO.setBuildCode(commonConfig.getBuildCode());
                commonConfigDTO.setDouyinAppToken(commonConfig.getDouyinAppToken());
                commonConfigDTO.setPayCardStyle(commonConfig.getPayCardStyle());
                commonConfigDTO.setHomeCardStyle(commonConfig.getHomeCardStyle());
                commonConfigDTO.setIaaMode(commonConfig.getIaaMode());
                commonConfigDTO.setReaderLoginType(commonConfig.getReaderLoginType());
                commonConfigDTO.setMineLoginType(commonConfig.getMineLoginType());


                // 查询是否存在
                AppCommonConfig existingCommonConfig = appCommonConfigService.getAppCommonConfig(appId);
                if (existingCommonConfig == null) {
                    sendLogToClient(currentTaskId, "[1-4] 新增通用配置...", CreateNovelLogType.PROCESSING);
                    appCommonConfigService.createAppCommonConfig(commonConfigDTO);
                    rollbackActions.add(() -> {
                        try { appCommonConfigService.deleteAppCommonConfig(appId); } catch (Exception ignore) {}
                    });
                    sendLogToClient(currentTaskId, "[1-4] 新增通用配置成功", CreateNovelLogType.SUCCESS);
                } else {
                    sendLogToClient(currentTaskId, "[1-4] 更新通用配置...", CreateNovelLogType.PROCESSING);
                    appCommonConfigService.updateAppCommonConfig(commonConfigDTO);
                    // 可选：记录原值用于回滚
                    sendLogToClient(currentTaskId, "[1-4] 更新通用配置成功", CreateNovelLogType.SUCCESS);
                }
                sendLogToClient(currentTaskId, "[1-4] app_common_config表更新成功", CreateNovelLogType.SUCCESS);

                //2.本地代码处理
                


                //3.本地图片资源处理


                

            } catch (Exception e) {

                sendLogToClient(currentTaskId, "创建小说小程序任务失败: " + e.getMessage(), CreateNovelLogType.ERROR);
                 // 回滚所有已完成的步骤
                for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                    try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
                }
            } finally {
                createNovelTaskManager.removeTask(currentTaskId);
                sendLogToClient(currentTaskId, "Create Novel Finish", CreateNovelLogType.FINISH);
            }
        });

        return result;
    }
}

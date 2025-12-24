package com.fun.novel.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import com.fun.novel.ai.entity.AppTheme;
import com.fun.novel.common.Result;
import com.fun.novel.entity.AppUIConfig;
import com.fun.novel.service.AppUIConfigService;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.service.NovelAppService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.context.Theme;

@Component
public class CreateAppTool implements ToolCallback {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(CreateAppTool.class);

    @Autowired
    private NovelAppService novelAppService;
    @Autowired
    private AppUIConfigService appUIConfigService;
    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("createNovelApp")
                .description("创建一个新的小说类小程序")
                .inputSchema("""
                    {
                      "type": "object",
                      "properties": {
                        "platform": {
                          "type": "string",
                          "description": "目标平台，可选值：douyin(抖音)、kuaishou(快手)、weixin(微信)、baidu(百度)",
                          "enum": ["douyin", "kuaishou", "weixin", "baidu"]
                        },
                        "appName": {
                          "type": "string",
                          "description": "小程序的名称"
                        }
                      },
                      "required": ["platform", "appName"]
                    }
                    """)
                .build();
    }
    
    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }
    
    @Override
    public String call(String toolInput) {
        try {
            JsonNode jsonNode = objectMapper.readTree(toolInput);
            String platform = jsonNode.get("platform").asText();
            String appName = jsonNode.get("appName").asText();
            logger.info(String.format("根据用户需求生成的小程序信息: %s", toolInput));
            logger.debug("Processing create app request for platform: {}, appName: {}", platform, appName);
            NovelApp appsByNameAndPlatform = novelAppService.getAppsByNameAndPlatform(appName, platform);
            if(appsByNameAndPlatform!= null){
                return objectMapper.writeValueAsString("已存在同名同平台的小程序，请重试");
            }
            StringBuilder result = new StringBuilder();

            result.append("开始创建小程序...\n");
            result.append(String.format("初始化%s平台的%s项目...\n", getPlatformName(platform), appName));

            // 创建小程序的多个步骤

            // 步骤1：生成基础配置
            CreateNovelAppRequest.BaseConfig baseConfig = generateAppBasicInfo(platform, appName, result);
            result.append("jsonStart-createNovelApp-baseConfig\n");
            result.append(baseConfig.toPrettyJson());
            result.append("jsonEnd-createNovelApp-baseConfig\n");
            result.append("基础配置生成完成!\n");

            // 步骤2：生成UI配置
            CreateNovelAppRequest.UiConfig uiConfig = generateUiConfig(platform, appName,result);
            result.append("jsonStart-createNovelApp-uiConfig\n");
            result.append(uiConfig.toPrettyJson());
            result.append("jsonEnd-createNovelApp-uiConfig\n");
            result.append("UI配置生成完成!\n");

            // 步骤3：生成支付配置
            CreateNovelAppRequest.PaymentConfig paymentConfig = generatePaymentConfig(platform, appName,result);
            result.append("jsonStart-createNovelApp-paymentConfig\n");
            result.append(paymentConfig.toPrettyJson());
            result.append("jsonEnd-createNovelApp-paymentConfig\n");
            result.append("支付配置生成完成!\n");
            
            // 步骤4：生成广告配置
            CreateNovelAppRequest.AdConfig adConfig = generateAdConfig(platform, appName,result);
            result.append("jsonStart-createNovelApp-adConfig\n");
            result.append(adConfig.toPrettyJson());
            result.append("jsonEnd-createNovelApp-adConfig\n");
            result.append("广告配置生成完成!\n");


            // 步骤5：生成通用配置

            CreateNovelAppRequest.CommonConfig commonConfig = generateCommonConfig(platform, appName,result);
            result.append("jsonStart-createNovelApp-commonConfig\n");
            result.append(commonConfig.toPrettyJson());
            result.append("jsonEnd-createNovelApp-commonConfig\n");
            result.append("通用配置生成完成!\n");

            return result.toString();
        } catch (Exception e) {
            logger.error("Error creating app", e);
            try {
                java.util.HashMap<String, Object> errorMap = new java.util.HashMap<>();
                errorMap.put("message", "创建小程序时出现错误: " + e.getMessage());
                errorMap.put("status", "error");
                return objectMapper.writeValueAsString(errorMap);
            } catch (Exception ex) {
                return "创建小程序时出现错误: " + e.getMessage();
            }
        }
    }



    /**
     * 步骤1：生成小程序基础信息
     *
     */
    private CreateNovelAppRequest.BaseConfig generateAppBasicInfo(String platform, String appName, StringBuilder result) {

        result.append("正在生成小程序基础配置...\n");
        
        // 生成小程序ID（使用UUID）
        String appId = generateAppId(platform);
        result.append(String.format("生成小程序ID: %s\n", appId));
        
        // 生成AppKey（针对需要的平台）
        if (needsAppKey(platform)) {
            String appKey = generateAppKey(appId);
            result.append(String.format("生成AppKey: %s\n", appKey));
        }
        
        CreateNovelAppRequest.BaseConfig baseConfig=new CreateNovelAppRequest.BaseConfig();
        baseConfig.setAppName(appName);
        baseConfig.setPlatform(platform);
        baseConfig.setAppid(appId);
        
        // 生成app_code
        String appCode = generateAppCode(platform, appName);
        baseConfig.setAppCode(appCode);
        
        baseConfig.setVersion("1.0.0");
        baseConfig.setTokenId(22);
        
        // 生成product和customer信息
        String productName = generateProductName(appName);
        String customerName = generateCustomerName(platform, appName);
        
        baseConfig.setProduct(productName);
        baseConfig.setCustomer(customerName);
        baseConfig.setCl(appCode);
        
        // 生成bannerId和deliverId（必填字段）
        baseConfig.setBannerId("test_mp_novel_business_type");
        baseConfig.setDeliverId("test_mp_novel_public_switch");
        
        logger.debug("Generated baseConfig: {}", baseConfig);
        return baseConfig;
    }
    /**
     * 步骤2：生成UI配置
     *
     */
    private CreateNovelAppRequest.UiConfig generateUiConfig(String platform, String appName,StringBuilder result) {
        result.append("正在生成小程序UI配置信息...\n");
        CreateNovelAppRequest.UiConfig uiConfig=new CreateNovelAppRequest.UiConfig();
        // 根据应用名称获取所有应用
        List<NovelApp> novelApps = novelAppService.getAppsByAppName(appName);

        // 如果没有找到应用，返回空数组
        if (novelApps == null || novelApps.isEmpty()) {
            //随机分配一个主题色
            result.append("随机分配一个主题色\n");
            // 预设主题色列表
            List<AppTheme> predefinedThemes = Arrays.asList(
                new AppTheme("阅界视窗主题", "#2552F5FF", "#DCE7FFFF",1,1),
                new AppTheme("悦动故事主题", "#EF5350FF", "#FFEBEEFF",1,1),
                new AppTheme("风行推广主题", "#F86003FF", "#FFEFE7FF",1,1),
                new AppTheme("漫影主题", "#FF4363FF", "#FFE5EBFF",1,1)
            );
            int randomIndex = ThreadLocalRandom.current().nextInt(0, predefinedThemes.size());
            uiConfig.setMainTheme(predefinedThemes.get(randomIndex).getMainTheme());
            uiConfig.setSecondTheme(predefinedThemes.get(randomIndex).getSecondTheme());
            uiConfig.setHomeCardStyle(predefinedThemes.get(randomIndex).getHomeCardStyle());
            uiConfig.setPayCardStyle(predefinedThemes.get(randomIndex).getPayCardStyle());
            result.append(String.format("随机分配主题色: %s\n", predefinedThemes.get(randomIndex).getName()));
        }else{
            //已经有其他平台同名Ui配置，直接应用
            result.append("已经有其他平台同名Ui配置，直接应用\n");
            AppUIConfig appUIConfig = appUIConfigService.getByAppId(novelApps.get(0).getAppid());
            uiConfig.setMainTheme(appUIConfig.getMainTheme());
            uiConfig.setSecondTheme(appUIConfig.getSecondTheme());
            uiConfig.setHomeCardStyle(appUIConfig.getHomeCardStyle());
            uiConfig.setPayCardStyle(appUIConfig.getPayCardStyle());
        }
        return uiConfig;
    }
    /**
     * 步骤3：生成支付配置
     *
     */
    private CreateNovelAppRequest.PaymentConfig generatePaymentConfig(String platform, String appName,StringBuilder result) {
        result.append("正在生成小程序支付配置信息...\n");
        CreateNovelAppRequest.PaymentConfig paymentConfig=new CreateNovelAppRequest.PaymentConfig();

        CreateNovelAppRequest.PayTypeConfig normalPay =new CreateNovelAppRequest.PayTypeConfig();
        normalPay.setEnabled(true);
        normalPay.setGatewayIos("1");
        normalPay.setGatewayAndroid("1");
        paymentConfig.setNormalPay(normalPay);

        CreateNovelAppRequest.PayTypeConfig orderPay =new CreateNovelAppRequest.PayTypeConfig();
        orderPay.setEnabled(false);
        orderPay.setGatewayIos("");
        orderPay.setGatewayAndroid("");
        paymentConfig.setOrderPay(orderPay);

        CreateNovelAppRequest.PayTypeConfig renewPay =new CreateNovelAppRequest.PayTypeConfig();
        renewPay.setEnabled(false);
        renewPay.setGatewayIos("");
        renewPay.setGatewayAndroid("");
        paymentConfig.setRenewPay(renewPay);

        CreateNovelAppRequest.PayTypeConfig douzuanPay =new CreateNovelAppRequest.PayTypeConfig();
        douzuanPay.setEnabled(false);
        douzuanPay.setGatewayIos("");
        douzuanPay.setGatewayAndroid("");
        paymentConfig.setDouzuanPay(douzuanPay);

        CreateNovelAppRequest.PayTypeConfig imPay =new CreateNovelAppRequest.PayTypeConfig();
        imPay.setEnabled(false);
        imPay.setGatewayIos("");
        imPay.setGatewayAndroid("");
        paymentConfig.setImPay(imPay);

        CreateNovelAppRequest.PayTypeConfig wxVirtualPay =new CreateNovelAppRequest.PayTypeConfig();
        wxVirtualPay.setEnabled(false);
        wxVirtualPay.setGatewayIos("");
        wxVirtualPay.setGatewayAndroid("");
        paymentConfig.setWxVirtualPay(wxVirtualPay);

        CreateNovelAppRequest.PayTypeConfig wxVirtualRenewPay =new CreateNovelAppRequest.PayTypeConfig();
        wxVirtualRenewPay.setEnabled(false);
        wxVirtualRenewPay.setGatewayIos("");
        wxVirtualRenewPay.setGatewayAndroid("");
        paymentConfig.setWxVirtualRenewPay(wxVirtualRenewPay);
        return paymentConfig;
    }

    /**
     * 步骤4：生成广告配置
     */
    private CreateNovelAppRequest.AdConfig generateAdConfig(String platform, String appName, StringBuilder result) {
        result.append("正在生成小程序广告配置信息...\n");
        CreateNovelAppRequest.AdConfig adConfig=new CreateNovelAppRequest.AdConfig();

        //激励广告配置
        CreateNovelAppRequest.RewardAdConfig rewardAdConfig = new CreateNovelAppRequest.RewardAdConfig();
        rewardAdConfig.setEnabled(true);
        rewardAdConfig.setRewardCount(10);
        rewardAdConfig.setRewardAdId("test");
        adConfig.setRewardAd(rewardAdConfig);

        //插屏广告
        CreateNovelAppRequest.InterstitialAdConfig interstitialAdConfig = new CreateNovelAppRequest.InterstitialAdConfig();
        interstitialAdConfig.setEnabled(false);
        adConfig.setInterstitialAd(interstitialAdConfig);

        //banner广告
        CreateNovelAppRequest.BannerAdConfig bannerAdConfig = new CreateNovelAppRequest.BannerAdConfig();
        bannerAdConfig.setEnabled(false);
        adConfig.setBannerAd(bannerAdConfig);

        //信息流广告
        CreateNovelAppRequest.FeedAdConfig feedAdConfig = new CreateNovelAppRequest.FeedAdConfig();
        feedAdConfig.setEnabled(false);
        adConfig.setFeedAd(feedAdConfig);

        return adConfig;

    }
    /**
     * 步骤5：生成通用配置
     */
    private CreateNovelAppRequest.CommonConfig generateCommonConfig(String platform, String appName, StringBuilder result) {
        result.append("正在生成小程序通用配置信息...\n");
        CreateNovelAppRequest.CommonConfig commonConfig=new CreateNovelAppRequest.CommonConfig();
        String buildCode = convertToPinyin(appName);
        commonConfig.setBuildCode(buildCode);
        commonConfig.setContact("test");
        commonConfig.setIaaMode(false);
        commonConfig.setIaaDialogStyle(1);
        commonConfig.setHidePayEntry(true);
        commonConfig.setHideScoreExchange(true);
        commonConfig.setMineLoginType("anonymousLogin");
        commonConfig.setReaderLoginType("anonymousLogin");
        return commonConfig;

    }

    /**
     * 生成小程序ID
     * 
     * @param platform 平台代码
     * @return 小程序ID
     */
    private String generateAppId(String platform) {
        // 根据平台生成不同格式的AppID
        String platformPrefix = getAppIdPrefix(platform);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return platformPrefix + uuid.substring(0, 16);
    }
    
    /**
     * 获取平台对应的AppID前缀
     * 
     * @param platform 平台代码
     * @return AppID前缀
     */
    private String getAppIdPrefix(String platform) {
        switch (platform) {
            case "douyin": return "dy_";
            case "kuaishou": return "ks_";
            case "weixin": return "wx_";
            case "baidu": return "bd_";
            default: return "app_";
        }
    }
    
    /**
     * 判断平台是否需要AppKey
     * 
     * @param platform 平台代码
     * @return 是否需要AppKey
     */
    private boolean needsAppKey(String platform) {
        // 根据实际平台需求配置
        return !"baidu".equals(platform);
    }
    
    /**
     * 生成AppKey
     * 
     * @param appId 小程序ID
     * @return AppKey
     */
    private String generateAppKey(String appId) {
        // 基于AppID生成AppKey（简单实现，实际项目中可以更复杂）
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String combined = appId + timestamp;
        return UUID.nameUUIDFromBytes(combined.getBytes()).toString().replace("-", "");
    }
    
    /**
     * 生成app_code
     * 格式：平台前缀_miniapp_拼音novel
     * 例如：tt_miniapp_xingchenwenjiannovel
     * 
     * @param platform 平台代码
     * @param appName 小程序名称
     * @return app_code
     */
    private String generateAppCode(String platform, String appName) {
        // 获取平台前缀
        String platformPrefix = getPlatformCodePrefix(platform);
        
        // 将appName转换为拼音
        String pinyinName = convertToPinyin(appName);
        
        // 组装app_code
        return String.format("%s_miniapp_%snovel", platformPrefix, pinyinName);
    }
    
    /**
     * 获取平台代码前缀
     * 
     * @param platform 平台代码
     * @return 平台前缀
     */
    private String getPlatformCodePrefix(String platform) {
        switch (platform) {
            case "douyin": return "tt";
            case "baidu": return "baidu";
            case "kuaishou": return "kuaishou";
            case "weixin": return "weixin";
            default: return "other";
        }
    }
    
    /**
     * 将中文转换为拼音，英文保持不变
     * 
     * @param text 要转换的文本
     * @return 拼音字符串
     */
    private String convertToPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder pinyin = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        
        try {
            for (char c : text.toCharArray()) {
                if (Character.isWhitespace(c)) {
                    continue;
                }
                if (c >= 0x4e00 && c <= 0x9fa5) {
                    // 汉字转换为拼音
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]);
                    }
                } else if (Character.isLetterOrDigit(c)) {
                    // 英文字母或数字保持不变
                    pinyin.append(c);
                }
                // 其他字符忽略
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            logger.error("Error converting to pinyin: {}", e.getMessage());
        }
        
        return pinyin.toString();
    }
    
    /**
     * 生成product名称
     * 格式：拼音
     * 例如：xingchenwenjiannovel
     * 
     * @param appName 小程序名称
     * @return product名称
     */
    private String generateProductName(String appName) {
        String pinyinName = convertToPinyin(appName);
        return pinyinName + "novel";
    }
    
    /**
     * 生成customer名称
     * 格式：平台前缀_拼音
     * 例如：tt_xingchenwenjiannovel
     * 
     * @param platform 平台代码
     * @param appName 小程序名称
     * @return customer名称
     */
    private String generateCustomerName(String platform, String appName) {
        String platformPrefix = getPlatformCodePrefix(platform);
        String pinyinName = convertToPinyin(appName);
        return String.format("%s_%snovel", platformPrefix, pinyinName);
    }


    
    /**
     * 步骤2：初始化项目结构
     * 
     * @param platform 平台代码
     * @param appName 小程序名称
     * @return 处理结果字符串
     */
    private String initializeProjectStructure(String platform, String appName) {
        StringBuilder sb = new StringBuilder();
        sb.append("配置小程序项目结构...\n");
        
        // TODO: 创建项目目录结构
        // TODO: 生成平台所需的配置文件
        
        logger.debug("Step 2 completed: Initialized project structure");
        return sb.toString();
    }
    

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }
    
    private String getPlatformName(String platformCode) {
        switch (platformCode) {
            case "douyin": return "抖音";
            case "kuaishou": return "快手";
            case "weixin": return "微信";
            case "baidu": return "百度";
            default: return platformCode;
        }
    }
}
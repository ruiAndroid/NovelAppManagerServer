package com.fun.novel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.checkerframework.checker.guieffect.qual.UI;

import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "创建小说小程序请求体")
public class CreateNovelAppRequest {
    @Schema(description = "基础配置")
    @JsonProperty("baseConfig")
    private BaseConfig baseConfig;

    @Schema(description = "支付配置")
    @JsonProperty("paymentConfig")
    private PaymentConfig paymentConfig;

    @Schema(description = "广告配置")
    @JsonProperty("adConfig")
    private AdConfig adConfig;

    @Schema(description = "通用配置")
    @JsonProperty("commonConfig")
    private CommonConfig commonConfig;

    @Schema(description = "UI配置")
    @JsonProperty("uiConfig")
    private UiConfig uiConfig;

    @Data
    public static class BaseConfig {
        @NotNull
        @Schema(description = "应用名称")
        private String appName;
        @NotNull
        @Schema(description = "应用编码")
        private String appCode;
        @NotNull
        @Schema(description = "平台类型")
        private String platform;
        @NotNull
        @Schema(description = "版本号")
        private String version;
        @NotNull
        @Schema(description = "产品")
        private String product;
        @NotNull
        @Schema(description = "客户")
        private String customer;
        @NotNull
        @Schema(description = "appid")
        private String appid;
        @NotNull
        @Schema(description = "tokenId")
        private Integer tokenId;
        @NotNull
        @Schema(description = "cl")
        private String cl;
        @NotNull
        @Schema(description = "bannerId")
        private String bannerId;
        @NotNull
        @Schema(description = "deliverId")
        private String deliverId;
    }


    @Data
    public static class PaymentConfig {
        private PayTypeConfig normalPay;
        private PayTypeConfig orderPay;
        private PayTypeConfig douzuanPay;
        private PayTypeConfig renewPay;
        private PayTypeConfig wxVirtualPay;
        private PayTypeConfig wxVirtualRenewPay;
        private PayTypeConfig imPay;
    }

    @Data
    public static class PayTypeConfig {
        private Boolean enabled;
        private String gatewayAndroid;
        private String gatewayIos;
    }

    @Data
    public static class AdConfig {
        private RewardAdConfig rewardAd;
        private InterstitialAdConfig interstitialAd;
        private BannerAdConfig bannerAd;
        private FeedAdConfig feedAd;
    }

    @Data
    public static class RewardAdConfig {
        private Boolean enabled;
        private String rewardAdId;
        private Integer rewardCount;
    }

    @Data
    public static class InterstitialAdConfig {
        private Boolean enabled;
        private String interstitialAdId;
        private Integer interstitialCount;
    }

    @Data
    public static class BannerAdConfig {
        private Boolean enabled;
        private String bannerAdId;
    }

    @Data
    public static class FeedAdConfig {
        private Boolean enabled;
        private String feedAdId;
    }

    @Data
    public static class CommonConfig {
        @NotNull
        @Schema(description = "构建号")
        private String buildCode;
        @NotNull
        @Schema(description = "联系方式")
        private String contact;
        @Schema(description = "抖音IM ID，可为空")
        private String douyinImId;
        @Schema(description = "快手AppToken，可为空")
        private String kuaishouAppToken;
        @NotNull
        @Schema(description = "快手ClientId")
        private String kuaishouClientId;
        @NotNull
        @Schema(description = "快手ClientSecret")
        private String kuaishouClientSecret;
        @NotNull
        @Schema(description = "我的页登录类型")
        private String mineLoginType;

        @NotNull
        @Schema(description = "阅读页登录类型")
        private String readerLoginType;
        @Schema(description = "微信AppToken，可为空")
        private String weixinAppToken;
        @Schema(description = "抖音AppToken")
        private String douyinAppToken;
        @Schema(description = "百度AppToken")
        private String baiduAppToken;
        @Schema(description = "是否Iaa模式")
        private Boolean iaaMode;
        @Schema(description = "iaa弹窗样式")
        private Integer iaaDialogStyle;
        @Schema(description = "是否屏蔽支付入口")
        private Boolean hidePayEntry;
        @Schema(description = "是否屏蔽移动积分入口")
        private Boolean hideScoreExchange;

    }

    @Data
    public static class UiConfig {
        @NotNull
        @Schema(description = "支付卡片样式")
        private Integer payCardStyle;

        @NotNull
        @Schema(description = "首页卡片样式")
        private Integer homeCardStyle;


        @NotNull
        @Schema(description = "主题色")
        private String mainTheme;

        @NotNull
        @Schema(description = "第二主题色")
        private String secondTheme;
    }
}
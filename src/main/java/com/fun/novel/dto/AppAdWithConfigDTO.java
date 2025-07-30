package com.fun.novel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "应用广告配置信息")
public class AppAdWithConfigDTO {
    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "激励广告配置")
    private RewardAdConfigDetail reward;

    @Schema(description = "插屏广告配置")
    private InterstitialAdConfigDetail interstitial;

    @Schema(description = "Banner广告配置")
    @JsonProperty("banner")
    private BannerAdConfigDetail banner;

    @Data
    @Schema(description = "奖励广告配置详情")
    public static class RewardAdConfigDetail {
        @Schema(description = "奖励广告ID")
        private String rewardAdId;

        @Schema(description = "奖励次数")
        private Integer rewardCount;

        @Schema(description = "是否启用")
        private Boolean isRewardAdEnabled;
    }

    @Data
    @Schema(description = "插屏广告配置详情")
    public static class InterstitialAdConfigDetail {
        @Schema(description = "插屏广告ID")
        private String interstitialAdId;

        @Schema(description = "插屏次数")
        private Integer interstitialCount;

        @Schema(description = "是否启用")
        private Boolean isInterstitialAdEnabled;
    }

    @Data
    @Schema(description = "Banner广告配置详情")
    public static class BannerAdConfigDetail {
        @Schema(description = "banner广告ID")
        private String bannerAdId;

        @Schema(description = "是否启用")
        private Boolean isBannerAdEnabled;
    }
} 
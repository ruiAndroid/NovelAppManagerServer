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

    @Schema(description = "奖励广告配置")
    private RewardAdConfigDetail reward;

    @Schema(description = "插屏广告配置")
    private InterstitialAdConfigDetail interstitial;

    @Schema(description = "原生广告配置")
    @JsonProperty("native")
    private NativeAdConfigDetail nativeAd;

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

    }

    @Data
    @Schema(description = "信息流广告配置详情")
    public static class NativeAdConfigDetail {

    }
} 
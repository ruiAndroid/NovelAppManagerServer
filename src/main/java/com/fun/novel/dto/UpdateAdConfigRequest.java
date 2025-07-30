package com.fun.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.*;

@Data
@Schema(description = "更新广告配置请求")
public class UpdateAdConfigRequest {
    @NotBlank(message = "广告类型不能为空")
    @Schema(description = "广告类型", required = true)
    private String adType;

    @NotBlank(message = "apAdId")
    @Schema(description = "apAdId", required = true)
    private String appAdId;

    @Schema(description = "激励广告ID")
    private String rewardAdId;

    @Min(value = 0, message = "激励次数不能为负数")
    @Schema(description = "激励次数")
    private Integer rewardCount;

    @Schema(description = "激励广告是否启用")
    private Boolean isRewardAdEnabled;


    @Schema(description = "插屏广告ID")
    private String interstitialAdId;

    @Min(value = 0, message = "插屏次数不能为负数")
    @Schema(description = "插屏广告展示次数")
    private Integer interstitialCount;

    @Schema(description = "插屏广告是否启用")
    private Boolean isInterstitialAdEnabled;

    @Schema(description = "Banner广告ID")
    private String bannerAdId;

    @Schema(description = "Banner广告是否启用")
    private Boolean isBannerAdEnabled;
} 
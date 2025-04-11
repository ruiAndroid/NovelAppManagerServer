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

    @Schema(description = "广告ID")
    private String rewardAdId;

    @Min(value = 0, message = "奖励次数不能为负数")
    @Schema(description = "奖励次数")
    private Integer rewardCount;

    @Schema(description = "是否启用")
    private Boolean isRewardAdEnabled;
} 
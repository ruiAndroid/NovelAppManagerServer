package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.*;

@Data
@TableName("ad_config")
@Schema(description = "广告配置")
public class AdConfig {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @NotNull(message = "app_ad_id不能为空")
    @Schema(description = "关联的AppAd ID")
    private Integer appAdId;

    @NotNull(message = "广告类型不能为空")
    @Schema(description = "广告类型")
    private String adType;

    @Schema(description = "奖励广告ID")
    private String rewardAdId;

    @Min(value = 0, message = "奖励次数不能为负数")
    @Schema(description = "奖励次数")
    private Integer rewardCount;

    @Schema(description = "奖励广告是否启用")
    private Boolean isRewardAdEnabled;
} 
package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

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
    @Schema(description = "插屏次数")
    private Integer interstitialCount;

    @Schema(description = "插屏广告是否启用")
    private Boolean isInterstitialAdEnabled;

    
    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonIgnore
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private LocalDateTime updateTime;

} 
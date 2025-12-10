package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fun.novel.validation.UniqueAppId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@TableName("app_ui_config")
@Schema(description = "UI配置")
public class AppUIConfig {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @NotBlank(message = "应用ID不能为空")
    @UniqueAppId
    @TableField("appid")
    @Schema(description = "应用ID")
    private String appId;

    // 新增字段 main_theme
    @Schema(description = "主主题")
    @TableField("main_theme")
    private String mainTheme;

    // 新增字段 second_theme
    @Schema(description = "副主题")
    @TableField("second_theme")
    private String secondTheme;

    @Schema(description = "支付卡片样式")
    @TableField("pay_card_style")
    private Integer payCardStyle;

    @Schema(description = "首页卡片样式")
    @TableField("home_card_style")
    private Integer homeCardStyle;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonIgnore
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private LocalDateTime updateTime;

} 
package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_common_config")
@Schema(description = "应用通用配置信息")
public class AppCommonConfig {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;
    
    @Schema(description = "应用ID")
    @TableField("appid")
    private String appId;
    
    @Schema(description = "联系方式")
    @TableField("contact")
    private String contact;
    
    @Schema(description = "抖音IM ID")
    @TableField("douyin_im_id")
    private String douyinImId;
    
    @Schema(description = "快手Client ID")
    @TableField("kuaishou_client_id")
    private String kuaishouClientId;
    
    @Schema(description = "快手Client Secret")
    @TableField("kuaishou_client_secret")
    private String kuaishouClientSecret;


    @Schema(description = "快手app token")
    @TableField("kuaishou_app_token")
    private String kuaishouAppToken;

    @Schema(description = "微信app token")
    @TableField("weixin_app_token")
    private String weixinAppToken;

    @Schema(description = "支付卡片样式")
    @TableField("pay_card_style")
    private Integer payCardStyle;
    
    @Schema(description = "首页卡片样式")
    @TableField("home_card_style")
    private Integer homeCardStyle;

    @Schema(description = "编译编码")
    @TableField("build_code")
    private String buildCode;

    @Schema(description = "抖音apptoken")
    @TableField("douyin_app_token")
    private String douyinAppToken;

    @Schema(description = "创建时间")
    @TableField("create_time")
    @JsonIgnore
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    @TableField("update_time")
    @JsonIgnore
    private LocalDateTime updateTime;
} 
package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    
    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;
} 
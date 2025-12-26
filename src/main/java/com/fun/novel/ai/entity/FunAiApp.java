package com.fun.novel.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI应用实体类
 */
@Data
@TableName("fun_ai_app")
public class FunAiApp {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "应用ID")
    private Long id;

    /**
     * 用户ID（外键）
     */
    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 应用名称
     */
    @TableField("app_name")
    @Schema(description = "应用名称")
    private String appName;

    /**
     * 应用描述
     */
    @TableField("app_description")
    @Schema(description = "应用描述")
    private String appDescription;

    /**
     * 应用类型（如小程序、网站等）
     */
    @TableField("app_type")
    @Schema(description = "应用类型")
    private String appType;

    /**
     * 应用状态（0：禁用，1：启用）
     */
    @TableField("app_status")
    @Schema(description = "应用状态")
    private Integer appStatus;

    /**
     * 最近一次部署失败原因（用于前端轮询时展示错误）
     */
    @TableField("last_deploy_error")
    @Schema(description = "最近一次部署失败原因（为空表示无错误）")
    private String lastDeployError;

    /**
     * 应用密钥
     */
    @TableField("app_key")
    @Schema(description = "应用密钥")
    private String appKey;

    /**
     * 应用密钥
     */
    @TableField("app_secret")
    @Schema(description = "应用密钥")
    @JsonIgnore
    private String appSecret;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonIgnore
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private LocalDateTime updateTime;

}
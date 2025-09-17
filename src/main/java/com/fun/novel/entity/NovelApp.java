package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fun.novel.validation.UniqueAppId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@TableName("novel_app")
@Schema(description = "小说应用信息")
public class NovelApp {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 255, message = "应用名称长度不能超过255个字符")
    @TableField("app_name")
    @Schema(description = "应用名称")
    private String appName;

    @NotBlank(message = "平台不能为空")
    @TableField("platform")
    @Schema(description = "平台")
    private String platform;

    @NotBlank(message = "应用代码不能为空")
    @TableField("app_code")
    @Schema(description = "应用代码")
    private String appCode;

    @Schema(description = "产品名称")
    @TableField("product")
    private String product;

    @Schema(description = "客户名称")
    @TableField("customer")
    private String customer;

    @NotBlank(message = "应用ID不能为空")
    @UniqueAppId
    @TableField("appid")
    @Schema(description = "应用ID")
    private String appid;

    @Schema(description = "Token ID")
    @TableField("token_id")
    private Integer tokenId;

    @Schema(description = "客户端类型")
    @TableField("cl")
    private String cl;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonIgnore
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private LocalDateTime updateTime;

    // deliver_id
    @Schema(description = "deliver_id")
    @TableField("deliver_id")
    private String deliverId;

    // banner_id
    @Schema(description = "banner_id")
    @TableField("banner_id")
    private String bannerId;

    // version 版本号
    @NotBlank(message = "version不能为空")
    @Schema(description = "version")
    @TableField("version")
    private String version;
}
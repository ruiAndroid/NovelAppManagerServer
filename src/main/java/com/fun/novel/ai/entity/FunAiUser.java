package com.fun.novel.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fun_ai_user")
public class FunAiUser {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户Id")
    private Long id;


    /**
     * 用户名
     */
    @TableField("user_name")
    @Schema(description = "用户名")
    private String userName;

    /**
     * 密码
     */
    @JsonIgnore
    @TableField("password")
    @Schema(description = "密码")
    private String password;

    /**
     * 头像
     */
    @TableField("avatar")
    @Schema(description = "用户头像")
    private String avatar;


    /**
     * 头像
     */
    @TableField("email")
    @Schema(description = "用户邮箱")
    private String email;
    /**
     * 手机号
     */
    @TableField("phone")
    @Schema(description = "手机号")
    private String phone;

    /**
     * 已创建应用数量
     */
    @TableField("app_count")
    @Schema(description = "已创建应用数量")
    private Integer appCount;

    /**
     * 最后登录时间
     */
    @JsonIgnore
    @Schema(description = "上次登录时间")
    @TableField(value = "last_login_time")
    private LocalDateTime lastLoginTime;

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
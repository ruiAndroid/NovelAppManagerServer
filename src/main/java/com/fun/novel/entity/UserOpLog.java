package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@TableName("user_op_log")
public class UserOpLog {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "log 主键Id")
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    @Schema(description = "用户id")
    private Long userId;

    /**
     * 用户id
     */
    @TableField("user_name")
    @Schema(description = "用户名")
    private String userName;

    /**
     * 用户的操作类型
     */
    @TableField("op_type")
    @Schema(description = "用户的操作类型")
    private Integer opType;

    /**
     * 操作状态
     */
    @JsonIgnore
    @TableField("op_status")
    @Schema(description = "操作状态")
    private Integer opStatus;

    /**
     * 操作的接口方法名
     */
    @TableField("method_name")
    @Schema(description = "操作的接口方法名")
    private String methodName;

    /**
     * 请求类型
     */
    @TableField("request_type")
    @Schema(description = "请求类型")
    private String requestType;


    /**
     * 请求的完整url
     */
    @TableField("request_url")
    @Schema(description = "url")
    private String requestUrl;

    /**
     * 请求的ip地址
     */
    @TableField("request_ip")
    @Schema(description = "请求的ip地址")
    private String requestIp;

    /**
     * 请求参数
     */
    @TableField("request_params")
    @Schema(description = "请求参数")
    private String requestParams;


    /**
     * 请求返回值
     */
    @TableField("response_result")
    @Schema(description = "请求返回值")
    private String responseResult;

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


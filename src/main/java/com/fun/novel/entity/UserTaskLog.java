package com.fun.novel.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_task_log")
@Schema(description = "用户任务日志")
public class UserTaskLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("task_id")
    @Schema(description = "任务ID")
    private Long taskId;

    @TableField("task_type")
    @Schema(description = "任务类型")
    private String taskType;

    @TableField("log_type")
    @Schema(description = "日志类型")
    private String logType;

    @TableField("message")
    @Schema(description = "日志内容")
    private String message;

    @TableField("log_data")
    @Schema(description = "日志详细数据")
    private String logData; // JSON格式存储

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonIgnore
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private LocalDateTime updateTime;
}
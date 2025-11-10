package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_template")
@Schema(description = "任务模板")
public class TaskTemplate {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("task_type")
    @Schema(description = "任务类型")
    private String taskType;

    @TableField("log_type")
    @Schema(description = "日志类型")
    private String logType;

    @TableField("display_name")
    @Schema(description = "显示名称")
    private String displayName;

    @TableField("template")
    @Schema(description = "模板配置")
    private String template; // JSON格式存储

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonIgnore
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private LocalDateTime updateTime;
}
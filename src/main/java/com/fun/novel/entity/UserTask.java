package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_task")
public class UserTask {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @TableField("id")
    @Schema(description = "log 主键Id")
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    @Schema(description = "用户id")
    private Long userId;

    /**
     * 任务类型
     */
    @TableField("task_type")
    @Schema(description = "任务类型")
    private String taskType;

    /**
     * 任务名称
     */
    @TableField("task_name")
    @Schema(description = "任务名称")
    private String taskName;

    /**
     * 当前任务状态
     */
    @TableField("task_status")
    @Schema(description = "任务的状态")
    private Integer opStatus;

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


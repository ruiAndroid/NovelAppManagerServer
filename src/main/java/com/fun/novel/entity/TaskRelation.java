package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_relation")
@Schema(description = "任务关系")
public class TaskRelation {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("parent_task_id")
    @Schema(description = "父任务ID")
    private Long parentTaskId;

    @TableField("child_task_id")
    @Schema(description = "子任务ID")
    private Long childTaskId;

    @TableField("relation_type")
    @Schema(description = "关系类型")
    private String relationType;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

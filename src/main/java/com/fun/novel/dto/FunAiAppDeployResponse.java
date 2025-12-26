package com.fun.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FunAI 应用部署结果")
public class FunAiAppDeployResponse {

    @Schema(description = "应用ID", example = "100")
    private Long appId;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "应用名称", example = "我的AI应用")
    private String appName;

    @Schema(description = "应用状态（1=空闲；2=部署中；3=启动中；4=运行中）", example = "4")
    private Integer appStatus;

    @Schema(description = "选中的zip包文件名", example = "upload_1703510400000_app.zip")
    private String zipFileName;

    @Schema(description = "部署目录（项目根目录，用于后续 build）", example = "D:\\\\...\\\\FunAiAppSpace\\\\1\\\\未命名应用1\\\\deploy")
    private String projectPath;
}



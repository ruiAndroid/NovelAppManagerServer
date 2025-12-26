package com.fun.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * FunAI 应用基础信息更新请求
 */
@Data
@Schema(description = "FunAI 应用基础信息更新请求")
public class UpdateFunAiAppBasicInfoRequest {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "应用ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long appId;

    @Schema(description = "应用名称（同一用户下唯一）", example = "我的AI应用")
    private String appName;

    @Schema(description = "应用描述", example = "这是一个很棒的应用")
    private String appDescription;

    @Schema(description = "应用类型", example = "website")
    private String appType;
}



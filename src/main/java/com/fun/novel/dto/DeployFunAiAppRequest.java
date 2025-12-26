package com.fun.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "部署 FunAI 应用请求")
public class DeployFunAiAppRequest {
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "应用ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long appId;
}



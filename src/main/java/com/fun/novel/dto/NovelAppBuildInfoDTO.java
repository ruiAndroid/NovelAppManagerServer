package com.fun.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "小程序构建信息")
public class NovelAppBuildInfoDTO {
    
    @Schema(description = "小程序名称")
    private String appName;
    
    @Schema(description = "平台列表")
    private List<PlatformInfo> platforms;
    
    @Data
    @Schema(description = "平台信息")
    public static class PlatformInfo {
        @Schema(description = "平台代码")
        private String platformCode;
        
        @Schema(description = "平台名称")
        private String platformName;
        
        @Schema(description = "构建时间")
        private String buildTime;
        
        @Schema(description = "构建版本")
        private String version;
        
        @Schema(description = "应用ID")
        private String appId;
        
        @Schema(description = "项目路径")
        private String projectPath;
    }
} 
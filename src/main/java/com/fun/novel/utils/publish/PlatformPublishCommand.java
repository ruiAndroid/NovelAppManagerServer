package com.fun.novel.utils.publish;

import java.util.Map;

/**
 * 平台发布命令接口
 */
public interface PlatformPublishCommand {
    /**
     * 构建发布命令
     * @param appId 应用ID
     * @param projectPath 项目路径
     * @param extraParams 额外参数（如抖音的token等）
     * @return 完整的发布命令
     */
    String buildCommand(String appId, String projectPath, Map<String, String> extraParams);

    /**
     * 获取平台代码
     * @return 平台代码
     */
    String getPlatformCode();

    /**
     * 获取平台名称
     * @return 平台名称
     */
    String getPlatformName();
} 
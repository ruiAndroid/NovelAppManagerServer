package com.fun.novel.utils.publish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

@Component
public class DouyinPublishCommand implements PlatformPublishCommand {
    private static final Logger logger = LoggerFactory.getLogger(DouyinPublishCommand.class);

    @Override
    public String buildCommand(String appId, String projectPath, Map<String, String> extraParams) {
        StringBuilder cmd = new StringBuilder();
        String token = extraParams.get("douyinAppToken");
        
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("抖音平台发布需要提供 douyinAppToken");
        }

        // 第一步：设置token
        cmd.append("npx @vue/cli-service uni-publish --platform mp-toutiao --token ").append(token);
        
        return cmd.toString();
    }

    @Override
    public String getPlatformCode() {
        return "mp-toutiao";
    }

    @Override
    public String getPlatformName() {
        return "抖音小程序";
    }

    /**
     * 处理发布过程中的输出
     * @param process 进程
     * @param taskId 任务ID
     * @return 是否成功
     */
    public boolean handleProcessOutput(Process process, String taskId) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean isSuccess = false;
            while ((line = reader.readLine()) != null) {
                logger.info("Publish log for task {}: {}", taskId, line);
                
                // 检查是否发布成功
                if (line.contains("发布成功")) {
                    isSuccess = true;
                }
                
                // 检查是否生成了二维码
                if (line.contains("二维码已生成")) {
                    // TODO: 处理二维码生成逻辑
                    logger.info("抖音小程序二维码已生成");
                }
            }
            return isSuccess;
        } catch (Exception e) {
            logger.error("处理发布输出失败", e);
            return false;
        }
    }
} 
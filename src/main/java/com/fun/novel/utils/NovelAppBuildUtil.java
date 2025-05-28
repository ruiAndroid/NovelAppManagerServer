package com.fun.novel.utils;

import com.fun.novel.websocket.BuildLogWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class NovelAppBuildUtil {
    private static final Logger logger = LoggerFactory.getLogger(NovelAppBuildUtil.class);
    private final BuildLogWebSocketHandler webSocketHandler;

    public NovelAppBuildUtil(BuildLogWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 异步构建uni-app项目
     * @param
     */
    public void buildNovelApp(String cmd) {
        CompletableFuture.runAsync(() -> {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String projectPath="D:\\dev\\h5\\code\\talos\\";
            // 设置工作目录
            processBuilder.directory(new File(projectPath));

            // 根据操作系统设置命令
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd.exe", "/c", cmd);
            } else {
                processBuilder.command("sh", "-c", cmd);
            }

            try {
                // 启动进程
                Process process = processBuilder.start();

                // 读取标准输出
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("Build log: {}", line);
                        // 通过WebSocket发送日志
                        webSocketHandler.sendBuildLog(line);
                    }
                }

                // 读取错误输出
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.error("Build error: {}", line);
                        String errorLog = "ERROR: " + line;
                        // 通过WebSocket发送错误日志
                        webSocketHandler.sendBuildLog(errorLog);
                    }
                }

                // 等待进程完成
                int exitCode = process.waitFor();
                String completionMessage;
                if (exitCode != 0) {
                    completionMessage = "Build failed with exit code: " + exitCode;
                    logger.error("Build process exited with code: {}", exitCode);
                } else {
                    completionMessage = "Build completed successfully";
                    logger.info("Build completed successfully");
                }
                webSocketHandler.sendBuildLog(completionMessage);

            } catch (Exception e) {
                String errorMessage = "Error during build process: " + e.getMessage();
                logger.error("Error during build process", e);
                webSocketHandler.sendBuildLog(errorMessage);
            }
        });
    }

    /**
     * 停止构建进程
     * @param process 构建进程
     */
    public void stopBuild(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
            logger.info("Build process stopped");
            webSocketHandler.sendBuildLog("Build process stopped");
        }
    }
} 
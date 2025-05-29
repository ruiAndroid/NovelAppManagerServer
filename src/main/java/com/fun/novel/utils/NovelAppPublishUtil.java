package com.fun.novel.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;

@Component
public class NovelAppPublishUtil {
    private static final Logger logger = LoggerFactory.getLogger(NovelAppPublishUtil.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private PublishTaskManager publishTaskManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 发布小程序到指定平台
     * @param platformCode 平台代码
     * @param appId 应用ID
     * @param projectPath 项目路径
     * @param douyinAppToken 抖音小程序Token（仅抖音平台需要）
     * @param version 版本号（仅抖音平台需要）
     * @param log 发布日志（仅抖音平台需要）
     * @return 任务ID，如果平台已有任务在运行则返回null
     */
    public String publishNovelApp(String platformCode, String appId, String projectPath, String douyinAppToken, String version, String log) {
        // 创建任务，检查平台是否已有任务在运行
        String taskId = publishTaskManager.createTask(platformCode);
        if (taskId == null) {
            String runningTaskId = publishTaskManager.getPlatformRunningTask(platformCode);
            logger.warn("平台 {} 已有任务 {} 在运行中，请稍后再试", platformCode, runningTaskId);
            return null;
        }

        CompletableFuture.runAsync(() -> {
            Process process = null;
            try {
                // 检查项目路径
                File projectDir = new File(projectPath);
                if (!projectDir.exists() || !projectDir.isDirectory()) {
                    throw new RuntimeException("项目路径不存在或不是目录: " + projectPath);
                }

                // 构建发布命令
                String publishCmd = buildPublishCommand(platformCode, appId, projectPath, douyinAppToken, version, log);
                logger.info("开始发布小程序，命令: {}", publishCmd);

                // 执行发布命令
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("cmd.exe", "/c", publishCmd);
                processBuilder.directory(projectDir);
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();
                publishTaskManager.addTask(taskId, process);

                // 读取命令输出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    boolean isSuccess = false;
                    boolean isTokenSet = false;
                    boolean hasError = false;
                    while ((line = reader.readLine()) != null) {
                        messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, line);
                        logger.info("Publish log for task {}: {}", taskId, line);

                        // 抖音平台特殊处理
                        if ("mp-toutiao".equals(platformCode)) {
                            // 检查是否有错误
                            if (line.startsWith("Upload Error")) {
                                hasError = true;
                                String errorMsg = "发布失败: " + line;
                                logger.error(errorMsg);
                                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, errorMsg);
                                break;
                            }

                            // 检查token是否设置成功
                            if (line.contains("Set app config success")) {
                                isTokenSet = true;
                                logger.info("抖音小程序token设置成功");
                                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Token设置成功，开始发布...");
                                
                                // 执行第二步：开始发布
                                String uploadCmd = buildUploadCommand(version, log, projectPath);
                                logger.info("开始上传小程序，命令: {}", uploadCmd);
                                
                                // 关闭当前进程
                                process.destroy();
                                
                                // 执行上传命令
                                processBuilder.command("cmd.exe", "/c", uploadCmd);
                                process = processBuilder.start();
                                publishTaskManager.addTask(taskId, process);

                                // 读取上传命令的输出
                                try (BufferedReader uploadReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                    String uploadLine;
                                    boolean isUploadSuccess = false;
                                    while ((uploadLine = uploadReader.readLine()) != null) {
                                        messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, uploadLine);
                                        logger.info("Upload log for task {}: {}", taskId, uploadLine);

                                        // 检查是否有错误
                                        if (uploadLine.startsWith("Upload Error")) {
                                            hasError = true;
                                            String errorMsg = "发布失败: " + uploadLine;
                                            logger.error(errorMsg);
                                            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, errorMsg);
                                            break;
                                        }

                                        // 检查是否上传成功
                                        if (uploadLine.contains("总体积")) {
                                            isUploadSuccess = true;
                                            logger.info("抖音小程序上传成功");
                                            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "上传成功，开始生成二维码...");
                                            
                                            // 执行第三步：生成二维码
                                            String previewCmd = buildPreviewCommand(projectPath);
                                            logger.info("开始生成二维码，命令: {}", previewCmd);
                                            
                                            // 关闭当前进程
                                            process.destroy();
                                            
                                            // 执行预览命令
                                            processBuilder.command("cmd.exe", "/c", previewCmd);
                                            process = processBuilder.start();
                                            publishTaskManager.addTask(taskId, process);

                                            // 读取预览命令的输出
                                            try (BufferedReader previewReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                                String previewLine;
                                                while ((previewLine = previewReader.readLine()) != null) {
                                                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, previewLine);
                                                    logger.info("Preview log for task {}: {}", taskId, previewLine);

                                                    // 检查是否有错误
                                                    if (previewLine.contains("Error")) {
                                                        hasError = true;
                                                        String errorMsg = "生成二维码失败: " + previewLine;
                                                        logger.error(errorMsg);
                                                        messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, errorMsg);
                                                        break;
                                                    }

                                                    // 提取二维码URL
                                                    if (previewLine.contains("二维码信息：")) {
                                                        String qrCodeUrl = previewLine.substring(previewLine.indexOf("二维码信息：") + 6);
                                                        logger.info("抖音小程序二维码生成成功: {}", qrCodeUrl);
                                                        messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "二维码生成成功: " + qrCodeUrl);
                                                        isSuccess = true;
                                                        // 成功生成二维码后直接标记为成功，不检查进程退出码
                                                        logger.info("发布成功完成，任务ID: {}", taskId);
                                                        messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish success");
                                                        return; // 直接返回，不等待进程退出
                                                    }
                                                }
                                            }
                                            break; // 退出上传命令的读取循环
                                        }
                                    }
                                }
                                break; // 退出第一个进程的读取循环
                            }
                        }
                    }

                    // 只有在没有成功生成二维码的情况下才检查进程退出码
                    if (process != null && !isSuccess) {
                        int exitCode = process.waitFor();
                        if (exitCode != 0 || hasError) {
                            String errorMsg = "发布失败，退出码: " + exitCode;
                            logger.error(errorMsg);
                            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error " + errorMsg);
                        }
                    }
                }
            } catch (Exception e) {
                String errorMsg = "发布过程发生错误: " + e.getMessage();
                logger.error(errorMsg);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error " + errorMsg);
            } finally {
                if (process != null) {
                    publishTaskManager.removeTask(taskId);
                }
            }
        }, executorService);

        return taskId;
    }

    /**
     * 构建上传命令
     */
    private String buildUploadCommand(String version, String log, String projectPath) {
        return String.format("tma upload -v %s -c \"%s\" %s", version, log, projectPath);
    }

    /**
     * 构建发布命令
     */
    private String buildPublishCommand(String platformCode, String appId, String projectPath, String douyinAppToken, String version, String log) {
        StringBuilder cmd = new StringBuilder();
        
        switch (platformCode) {
            case "mp-weixin":
                cmd.append("npx @vue/cli-service uni-publish --platform mp-weixin");
                break;
            case "mp-toutiao":
                // 抖音平台三步发布流程
                if (douyinAppToken == null || douyinAppToken.trim().isEmpty()) {
                    throw new IllegalArgumentException("抖音平台发布需要提供 douyinAppToken");
                }
                // 1. 设置token
                cmd.append("tma set-app-config ").append(appId).append(" --token ").append(douyinAppToken);
                break;
            case "mp-kuaishou":
                cmd.append("npx @vue/cli-service uni-publish --platform mp-kuaishou");
                break;
            case "mp-baidu":
                cmd.append("npx @vue/cli-service uni-publish --platform mp-baidu");
                break;
            default:
                throw new IllegalArgumentException("不支持的平台代码: " + platformCode);
        }
        
        return cmd.toString();
    }

    /**
     * 构建预览命令
     */
    private String buildPreviewCommand(String projectPath) {
        return "tma preview " + projectPath;
    }

    /**
     * 停止发布任务
     */
    public void stopPublish(String taskId) {
        publishTaskManager.stopTask(taskId);
    }

    @PreDestroy
    public void cleanup() {
        logger.info("关闭发布工具线程池...");
        executorService.shutdown();
    }
} 
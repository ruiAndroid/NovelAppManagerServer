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
public class NovelAppBuildUtil {
    private static final Logger logger = LoggerFactory.getLogger(NovelAppBuildUtil.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private BuildTaskManager taskManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public String buildNovelApp(String cmd) {
        String taskId = taskManager.createTask();
        CompletableFuture.runAsync(() -> {
            Process process = null;
            try {
                String workPath="D:\\dev\\h5\\code\\talos\\";
                // 设置工作目录为uni-app项目根目录
                File workDir = new File(workPath);
                if (!workDir.exists()) {
                    throw new RuntimeException("工作目录不存在: " + workDir.getAbsolutePath());
                }

                logger.info("start Build cmd: {},workPath:{}",cmd, workPath);
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("cmd.exe", "/c", cmd);
                processBuilder.directory(workDir);
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();
                taskManager.addTask(taskId, process);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        messagingTemplate.convertAndSend("/topic/build-logs/" + taskId, line);
                        logger.info("Build log for task {}: {}", taskId, line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    String errorMsg = "Build process for task " + taskId + " exited with code: " + exitCode;
                    logger.error(errorMsg);
                    messagingTemplate.convertAndSend("/topic/build-logs/" + taskId, "ERROR: " + errorMsg);
                }
            } catch (Exception e) {
                String errorMsg = "Build error for task " + taskId + ": " + e.getMessage();
                logger.error(errorMsg);
                messagingTemplate.convertAndSend("/topic/build-logs/" + taskId, "ERROR: " + errorMsg);
            } finally {
                if (process != null) {
                    taskManager.removeTask(taskId);
                }
            }
        }, executorService);

        return taskId;
    }

    public void stopBuild(String taskId) {
        taskManager.stopTask(taskId);
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down executor service...");
        executorService.shutdown();
    }
} 
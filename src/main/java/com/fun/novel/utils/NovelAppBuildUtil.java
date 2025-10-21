package com.fun.novel.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;

@Component
public class NovelAppBuildUtil {
    private static final Logger logger = LoggerFactory.getLogger(NovelAppBuildUtil.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Set<String> executingCommands = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    private BuildTaskManager taskManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${build.workPath}")
    private String workPath;


    @Value("${build.buildedPath}")
    private String buildedPath;
    public String getBuildedPath() {
        return buildedPath;
    }

    public String buildNovelApp(String cmd) {
        if (cmd == null || cmd.isEmpty()) {
            throw new IllegalArgumentException("构建命令不能为空");
        }

        synchronized (executingCommands) {
            if (executingCommands.contains(cmd)) {
                throw new IllegalStateException("构建命令 " + cmd + " 已经在执行中，请勿重复提交");
            }
            executingCommands.add(cmd);
        }

        String taskId = taskManager.createTask();
        CompletableFuture.runAsync(() -> {

            try {
                // 给前端一些时间建立WebSocket连接
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
            Process process = null;
            try {
                // 检查工作目录是否存在
                File workDir = new File(workPath);
                if (!workDir.exists()) {
                    throw new RuntimeException("工作目录不存在: " + workDir.getAbsolutePath());
                }

                logger.info("start Build cmd: {},workPath:{}", cmd, workPath);
                ProcessBuilder processBuilder = new ProcessBuilder();
                
                // 根据操作系统类型选择合适的命令执行方式
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("win")) {
                    // Windows系统使用cmd.exe
                    processBuilder.command("cmd.exe", "/c", cmd);
                } else {
                    // macOS/Linux系统使用shell
                    processBuilder.command("sh", "-c", cmd);
                }
                
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
                    String errorMsg = "Build error for task " + taskId + " exited with code: " + exitCode;
                    logger.error(errorMsg);
                    messagingTemplate.convertAndSend("/topic/build-logs/" + taskId, "ERROR: " + errorMsg);
                } else {
                    logger.info("Build completed successfully for task {}", taskId);
                    messagingTemplate.convertAndSend("/topic/build-logs/" + taskId, "Build completed successfully");
                }
            } catch (Exception e) {
                String errorMsg = "Build error for task " + taskId + ": " + e.getMessage();
                logger.error(errorMsg);
                messagingTemplate.convertAndSend("/topic/build-logs/" + taskId, "ERROR: " + errorMsg);
            } finally {
                if (process != null) {
                    taskManager.removeTask(taskId);
                }
                synchronized (executingCommands) {
                    executingCommands.remove(cmd);
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
        logger.info("关闭构建工具线程池...");
        executorService.shutdown();
    }
}
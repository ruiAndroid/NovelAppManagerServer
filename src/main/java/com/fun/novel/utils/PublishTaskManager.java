package com.fun.novel.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class PublishTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(PublishTaskManager.class);
    private final Map<String, Process> runningTasks = new ConcurrentHashMap<>();
    private final Map<String, String> platformRunningTasks = new ConcurrentHashMap<>();
    private final Map<String, String> taskPlatforms = new ConcurrentHashMap<>();
    private final Map<String, String> taskProjectPaths = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * 创建发布任务
     * @param platformCode 平台代码
     * @return 任务ID，如果平台已有任务在运行则返回null
     */
    public String createTask(String platformCode) {
        String taskId = UUID.randomUUID().toString();
        if (platformRunningTasks.putIfAbsent(platformCode, taskId) == null) {
            taskPlatforms.put(taskId, platformCode);
            logger.info("创建发布任务: {}, 平台: {}", taskId, platformCode);
            return taskId;
        }
        logger.warn("平台 {} 已有任务在运行", platformCode);
        return null;
    }

    /**
     * 添加任务
     * @param taskId 任务ID
     * @param process 进程
     */
    public void addTask(String taskId, Process process) {
        runningTasks.put(taskId, process);
        logger.info("添加发布任务: {}", taskId);
    }

    /**
     * 获取任务
     * @param taskId 任务ID
     * @return 进程
     */
    public Process getTask(String taskId) {
        return runningTasks.get(taskId);
    }

    /**
     * 移除任务
     * @param taskId 任务ID
     */
    public void removeTask(String taskId) {
        Process process = runningTasks.remove(taskId);
        if (process != null) {
            logger.info("尝试停止并移除发布任务进程: {}", taskId);
            try {
                if (process.isAlive()) {
                    logger.info("正在终止进程: {}", taskId);
                    process.destroy();
                    // Wait for process to exit
                    if (!process.waitFor(10, TimeUnit.SECONDS)) { // Increased wait time
                        logger.warn("进程未能在10秒内终止，强制终止: {}", taskId);
                        process.destroyForcibly();
                         if (!process.waitFor(10, TimeUnit.SECONDS)) { // Wait again after forceful destruction
                             logger.warn("进程强制终止后{}秒内仍未停止: {}", 10, taskId);
                         }
                    }
                }
            } catch (Exception e) {
                logger.error("停止发布任务进程失败 {}: {}", taskId, e.getMessage());
            }
             if (process != null && process.isAlive()) {
                 logger.warn("进程 {} 停止失败，可能仍在运行", taskId);
             }
        }
        
        // 从平台任务映射中移除
        platformRunningTasks.entrySet().removeIf(entry -> entry.getValue().equals(taskId));
        
        // 延迟移除任务信息，以便前端可以获取二维码
        scheduler.schedule(() -> {
            String platformCode = taskPlatforms.remove(taskId);
            taskProjectPaths.remove(taskId);
            logger.info("延迟移除发布任务信息: {}", taskId);
        }, 5, TimeUnit.MINUTES);
        
        logger.info("任务进程处理完成: {}", taskId);
    }

    /**
     * 停止任务
     * @param taskId 任务ID
     */
    public void stopTask(String taskId) {
        logger.info("开始停止任务（通过removeTask处理进程终止）: {}", taskId);
        removeTask(taskId); // Delegate process termination to removeTask
        logger.info("停止任务指令已发送: {}", taskId);
    }

    /**
     * 获取平台当前运行的任务ID
     * @param platformCode 平台代码
     * @return 任务ID，如果没有任务在运行则返回null
     */
    public String getPlatformRunningTask(String platformCode) {
        return platformRunningTasks.get(platformCode);
    }

    public String getPlatformCode(String taskId) {
        return taskPlatforms.get(taskId);
    }

    public void setProjectPath(String taskId, String projectPath) {
        taskProjectPaths.put(taskId, projectPath);
    }

    public String getProjectPath(String taskId) {
        return taskProjectPaths.get(taskId);
    }

    /**
     * 清理所有任务
     */
    @PreDestroy
    public void cleanup() {
        logger.info("清理所有发布任务...");
        runningTasks.forEach((taskId, process) -> {
            try {
                if (process != null && process.isAlive()) {
                    process.destroyForcibly(); // Forcefully destroy on cleanup
                }
            } catch (Exception e) {
                logger.error("清理发布任务失败 {}: {}", taskId, e.getMessage());
            }
        });
        runningTasks.clear();
        platformRunningTasks.clear();
        taskPlatforms.clear();
        taskProjectPaths.clear();
        scheduler.shutdown();
        logger.info("所有发布任务已清理");
    }
} 
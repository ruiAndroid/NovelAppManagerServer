package com.fun.novel.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Component
public class PublishTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(PublishTaskManager.class);
    private final Map<String, Process> runningTasks = new ConcurrentHashMap<>();
    private final Map<String, String> platformRunningTasks = new ConcurrentHashMap<>();

    /**
     * 创建发布任务
     * @param platformCode 平台代码
     * @return 任务ID，如果平台已有任务在运行则返回null
     */
    public String createTask(String platformCode) {
        // 检查平台是否已有任务在运行
        if (platformRunningTasks.containsKey(platformCode)) {
            String runningTaskId = platformRunningTasks.get(platformCode);
            logger.warn("平台 {} 已有任务 {} 在运行中", platformCode, runningTaskId);
            return null;
        }

        String taskId = UUID.randomUUID().toString();
        platformRunningTasks.put(platformCode, taskId);
        logger.info("创建发布任务: {}, 平台: {}", taskId, platformCode);
        return taskId;
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
        if (process != null && process.isAlive()) {
            try {
                process.destroy();
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                logger.error("移除发布任务失败 {}: {}", taskId, e.getMessage());
            }
        }
        
        // 从平台任务映射中移除
        platformRunningTasks.entrySet().removeIf(entry -> entry.getValue().equals(taskId));
        logger.info("移除发布任务: {}", taskId);
    }

    /**
     * 停止任务
     * @param taskId 任务ID
     */
    public void stopTask(String taskId) {
        Process process = runningTasks.get(taskId);
        if (process != null && process.isAlive()) {
            try {
                process.destroy();
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                logger.error("停止发布任务失败 {}: {}", taskId, e.getMessage());
            }
        }
        runningTasks.remove(taskId);
        
        // 从平台任务映射中移除
        platformRunningTasks.entrySet().removeIf(entry -> entry.getValue().equals(taskId));
        logger.info("停止发布任务: {}", taskId);
    }

    /**
     * 获取平台当前运行的任务ID
     * @param platformCode 平台代码
     * @return 任务ID，如果没有任务在运行则返回null
     */
    public String getPlatformRunningTask(String platformCode) {
        return platformRunningTasks.get(platformCode);
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
                    process.destroy();
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                }
            } catch (Exception e) {
                logger.error("清理发布任务失败 {}: {}", taskId, e.getMessage());
            }
        });
        runningTasks.clear();
        platformRunningTasks.clear();
        logger.info("所有发布任务已清理");
    }
} 
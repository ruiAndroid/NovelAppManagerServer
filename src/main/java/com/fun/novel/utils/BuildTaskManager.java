package com.fun.novel.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Component
public class BuildTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(BuildTaskManager.class);
    private final Map<String, Process> runningTasks = new ConcurrentHashMap<>();

    public String createTask() {
        String taskId = UUID.randomUUID().toString();
        return taskId;
    }

    public void addTask(String taskId, Process process) {
        runningTasks.put(taskId, process);
    }

    public Process getTask(String taskId) {
        return runningTasks.get(taskId);
    }

    public void removeTask(String taskId) {
        Process process = runningTasks.remove(taskId);
        if (process != null && process.isAlive()) {
            try {
                process.destroy();
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                logger.error("Error destroying process for task {}: {}", taskId, e.getMessage());
            }
        }
    }

    public void stopTask(String taskId) {
        Process process = runningTasks.get(taskId);
        if (process != null && process.isAlive()) {
            try {
                process.destroy();
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                runningTasks.remove(taskId);
                logger.info("Successfully stopped task: {}", taskId);
            } catch (Exception e) {
                logger.error("Error stopping task {}: {}", taskId, e.getMessage());
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up all running tasks...");
        runningTasks.forEach((taskId, process) -> {
            try {
                if (process != null && process.isAlive()) {
                    process.destroy();
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                }
            } catch (Exception e) {
                logger.error("Error cleaning up task {}: {}", taskId, e.getMessage());
            }
        });
        runningTasks.clear();
        logger.info("All tasks cleaned up");
    }
} 
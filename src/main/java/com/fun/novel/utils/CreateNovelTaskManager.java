package com.fun.novel.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CreateNovelTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(CreateNovelTaskManager.class);
    // 当前唯一任务ID
    private final AtomicReference<String> currentTaskId = new AtomicReference<>(null);

    /**
     * 创建任务，只允许一个任务存在
     * @return 任务ID，若已有任务则返回null
     */
    public String createTask() {
        String taskId = UUID.randomUUID().toString();
        if (currentTaskId.compareAndSet(null, taskId)) {
            logger.info("创建NovelCreate任务: {}", taskId);
            return taskId;
        }
        logger.warn("已有NovelCreate任务在运行: {}", currentTaskId.get());
        return null;
    }

    /**
     * 移除任务
     * @param taskId 任务ID
     */
    public void removeTask(String taskId) {
        if (currentTaskId.compareAndSet(taskId, null)) {
            logger.info("移除NovelCreate任务: {}", taskId);
        } else {
            logger.warn("尝试移除任务失败，当前任务ID: {}，请求移除ID: {}", currentTaskId.get(), taskId);
        }
    }

    /**
     * 获取当前任务ID
     * @return 当前任务ID，若无则为null
     */
    public String getCurrentTaskId() {
        return currentTaskId.get();
    }
} 
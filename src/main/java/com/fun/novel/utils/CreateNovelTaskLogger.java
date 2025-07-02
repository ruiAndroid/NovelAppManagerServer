package com.fun.novel.utils;

import com.fun.novel.dto.CreateNovelLogMessage;
import com.fun.novel.dto.CreateNovelLogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class CreateNovelTaskLogger {
    private static final Logger logger = LoggerFactory.getLogger(CreateNovelTaskLogger.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void log(String taskId, String message, CreateNovelLogType type) {
        String destination = "/topic/novel-create-log/" + taskId;
        messagingTemplate.convertAndSend(destination, CreateNovelLogMessage.from(message, type));
        logger.info("[NovelCreateLog] taskId={} message={}", taskId, message);
    }
} 
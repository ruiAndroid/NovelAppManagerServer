package com.fun.novel.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class BuildLogWebSocketHandler {
    private final SimpMessagingTemplate messagingTemplate;

    public BuildLogWebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendBuildLog(String taskId, String log) {
        messagingTemplate.convertAndSend("/topic/build-logs/" + taskId, log);
    }
} 
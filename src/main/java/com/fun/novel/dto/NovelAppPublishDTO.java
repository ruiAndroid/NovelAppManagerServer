package com.fun.novel.dto;

public class NovelAppPublishDTO {
    private String taskId;

    public NovelAppPublishDTO() {
    }

    public NovelAppPublishDTO(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
} 
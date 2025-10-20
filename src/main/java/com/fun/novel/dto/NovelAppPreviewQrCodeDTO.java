package com.fun.novel.dto;

public class NovelAppPreviewQrCodeDTO {
    private String taskId;

    public NovelAppPreviewQrCodeDTO() {
    }

    public NovelAppPreviewQrCodeDTO(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
} 
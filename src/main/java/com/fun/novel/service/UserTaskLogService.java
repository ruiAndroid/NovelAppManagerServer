package com.fun.novel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fun.novel.entity.UserTaskLog;

import java.util.List;

public interface UserTaskLogService extends IService<UserTaskLog> {
    /**
     * 创建任务日志
     * @param taskId 任务ID
     * @param taskType 任务类型
     * @param logType 日志类型
     * @param message 日志消息
     * @return UserTaskLog
     */
    UserTaskLog createTaskLog(Long taskId, String taskType, String logType, String message,String logData);

    /**
     * 批量创建任务日志
     * @param taskLogs 任务日志列表
     */
    void batchCreateTaskLogs(List<UserTaskLog> taskLogs);

    /**
     * 根据任务ID获取任务日志
     * @param taskId 任务ID
     * @return 任务日志列表
     */
    List<UserTaskLog> getTaskLogsByTaskId(Long taskId);

    /**
     * 根据任务ID和日志类型获取任务日志
     * @param taskId 任务ID
     * @param logType 日志类型
     * @return 任务日志列表
     */
    List<UserTaskLog> getTaskLogsByTaskIdAndLogType(Long taskId, String logType);
}
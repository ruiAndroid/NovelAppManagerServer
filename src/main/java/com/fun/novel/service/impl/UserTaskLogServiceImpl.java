package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.UserTaskLog;
import com.fun.novel.mapper.UserTaskLogMapper;
import com.fun.novel.service.UserTaskLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserTaskLogServiceImpl extends ServiceImpl<UserTaskLogMapper, UserTaskLog> implements UserTaskLogService {

    @Override
    public UserTaskLog createTaskLog(Long mainTaskId, String taskType, String logType, String message,String logData) {
        UserTaskLog taskLog = new UserTaskLog();
        //根据mainTaskId以及logType查询子任务id

//        taskLog.setTaskId(taskId);
        taskLog.setTaskType(taskType);
        taskLog.setLogType(logType);
        taskLog.setMessage(message);
        save(taskLog);
        return taskLog;
    }

    @Override
    public void batchCreateTaskLogs(List<UserTaskLog> taskLogs) {
        saveBatch(taskLogs);
    }

    @Override
    public List<UserTaskLog> getTaskLogsByTaskId(Long taskId) {
        QueryWrapper<UserTaskLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        return list(queryWrapper);
    }

    @Override
    public List<UserTaskLog> getTaskLogsByTaskIdAndLogType(Long taskId, String logType) {
        QueryWrapper<UserTaskLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.eq("log_type", logType);
        return list(queryWrapper);
    }
}
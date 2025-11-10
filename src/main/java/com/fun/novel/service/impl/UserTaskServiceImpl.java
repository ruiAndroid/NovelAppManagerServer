package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.UserTask;
import com.fun.novel.mapper.UserTaskMapper;
import com.fun.novel.service.UserTaskService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserTaskServiceImpl extends ServiceImpl<UserTaskMapper, UserTask> implements UserTaskService {

    @Override
    public boolean saveTask(UserTask task) {
        return save(task);
    }

    @Override
    public boolean saveBatchTasks(List<UserTask> tasks) {
        return saveBatch(tasks);
    }

    @Override
    public List<UserTask> getTasksByUserId(Long userId) {
        return lambdaQuery().eq(UserTask::getUserId, userId).list();
    }

    @Override
    public UserTask getTaskById(Long taskId) {
        return getById(taskId);
    }
}
package com.fun.novel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fun.novel.entity.UserTask;

import java.util.List;

public interface UserTaskService  extends IService<UserTask> {
    /**
     * 保存任务
     */
    boolean saveTask(UserTask task);

    /**
     * 批量保存任务
     */
    boolean saveBatchTasks(List<UserTask> tasks);

    /**
     * 根据用户ID获取任务列表
     */
    List<UserTask> getTasksByUserId(Long userId);

    /**
     * 根据任务ID获取任务
     */
    UserTask getTaskById(Long taskId);

}

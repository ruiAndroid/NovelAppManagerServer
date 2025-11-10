package com.fun.novel.service.impl;

import com.fun.novel.entity.TaskRelation;
import com.fun.novel.entity.UserTask;
import com.fun.novel.service.TaskRelationService;
import com.fun.novel.service.UserTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务管理实现类
 */
@Service
public class TaskManagementService {

    @Autowired
    private UserTaskService userTaskService;

    @Autowired
    private TaskRelationService taskRelationService;



    /**
     * 创建主任务和子任务
     */
    public Long createTaskWithSubTasks(Long userId, String appId, String platformCode, String taskType, String taskName) {
        // 创建主任务
        UserTask mainTask = new UserTask();
        mainTask.setUserId(userId);
        mainTask.setAppid(appId);
        mainTask.setTaskType(taskType);
        mainTask.setTaskName(taskName + " (主任务)");
        mainTask.setTaskStatus("RUNNING");
        mainTask.setStartTime(LocalDateTime.now());
        userTaskService.saveTask(mainTask);

        Long mainTaskId = mainTask.getId();

        // 1.创建子任务
        List<UserTask> subTasks = createSubTasks(userId, appId, platformCode, taskType, mainTaskId);

        // 2.批量保存子任务
        userTaskService.saveBatchTasks(subTasks);

        // 3.创建任务关系
        List<TaskRelation> relations = new ArrayList<>();
        for (UserTask subTask : subTasks) {
            TaskRelation relation = new TaskRelation();
            relation.setParentTaskId(mainTaskId);
            relation.setChildTaskId(subTask.getId());
            relation.setRelationType("PARENT_CHILD");
            relations.add(relation);
        }
        taskRelationService.batchCreateTaskRelations(relations);

        return mainTaskId;
    }

    /**
     * 根据主任务类型创建子任务
     */
    private List<UserTask> createSubTasks(Long userId, String appId, String platformCode, String taskType, Long parentTaskId) {
        List<UserTask> subTasks = new ArrayList<>();

        if ("CREATE".equals(taskType)) {
            subTasks.add(createTask(userId, appId, "CREATE", "create_base_config", "创建基础配置"));
            subTasks.add(createTask(userId, appId, "CREATE", "create_ui_config", "创建UI配置"));
            subTasks.add(createTask(userId, appId, "CREATE", "create_pay_config", "创建支付配置"));
            subTasks.add(createTask(userId, appId, "CREATE", "create_ad_config", "创建广告配置"));
        } else if ("BUILD".equals(taskType)) {
            subTasks.add(createTask(userId, appId, "BUILD", "build_init", "构建初始化"));
            subTasks.add(createTask(userId, appId, "BUILD", "build_compile", "编译代码"));
            subTasks.add(createTask(userId, appId, "BUILD", "build_package", "打包应用"));
        } else if ("PREVIEW".equals(taskType)) {
            subTasks.add(createTask(userId, appId, "PREVIEW", "PREVIEW_START", "开始生成预览码"));
            subTasks.add(createTask(userId, appId, "PREVIEW", "PREVIEW_GENERATING", "预览码生成中"));
            subTasks.add(createTask(userId, appId, "PREVIEW", "PREVIEW_SUCCESS", "预览码生成成功"));
            subTasks.add(createTask(userId, appId, "PREVIEW", "PREVIEW_ERROR", "预览码生成失败"));
        }

        return subTasks;
    }



    private UserTask createTask(Long userId, String appId, String taskType, String taskSubType, String taskName) {
        UserTask task = new UserTask();
        task.setUserId(userId);
        task.setAppid(appId);
        task.setTaskType(taskSubType);
        task.setTaskName(taskName);
        task.setTaskStatus("PENDING");
        return task;
    }



}
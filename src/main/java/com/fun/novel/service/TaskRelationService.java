package com.fun.novel.service;

import com.fun.novel.entity.TaskRelation;
import java.util.List;

public interface TaskRelationService {
    /**
     * 创建任务关系
     * @param taskRelation 任务关系实体
     * @return 保存后的任务关系
     */
    TaskRelation createTaskRelation(TaskRelation taskRelation);

    /**
     * 批量创建任务关系
     * @param taskRelations 任务关系列表
     */
    void batchCreateTaskRelations(List<TaskRelation> taskRelations);

    /**
     * 根据父任务ID获取所有子任务关系
     * @param parentTaskId 父任务ID
     * @return 任务关系列表
     */
    List<TaskRelation> getTaskRelationsByParentTaskId(Long parentTaskId);

    /**
     * 根据子任务ID获取所有父任务关系
     * @param childTaskId 子任务ID
     * @return 任务关系列表
     */
    List<TaskRelation> getTaskRelationsByChildTaskId(Long childTaskId);

    /**
     * 根据父任务ID和子任务ID获取任务关系
     * @param parentTaskId 父任务ID
     * @param childTaskId 子任务ID
     * @return 任务关系
     */
    TaskRelation getTaskRelation(Long parentTaskId, Long childTaskId);

    /**
     * 删除任务关系
     * @param id 任务关系ID
     * @return 是否删除成功
     */
    boolean deleteTaskRelation(Long id);

    /**
     * 根据父任务ID删除所有相关任务关系
     * @param parentTaskId 父任务ID
     * @return 删除的记录数
     */
    int deleteTaskRelationsByParentTaskId(Long parentTaskId);

    /**
     * 根据子任务ID删除所有相关任务关系
     * @param childTaskId 子任务ID
     * @return 删除的记录数
     */
    int deleteTaskRelationsByChildTaskId(Long childTaskId);
}
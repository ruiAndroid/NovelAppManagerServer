package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.TaskRelation;
import com.fun.novel.mapper.TaskRelationMapper;
import com.fun.novel.service.TaskRelationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskRelationServiceImpl extends ServiceImpl<TaskRelationMapper, TaskRelation> implements TaskRelationService {

    @Override
    public TaskRelation createTaskRelation(TaskRelation taskRelation) {
        save(taskRelation);
        return taskRelation;
    }

    @Override
    public void batchCreateTaskRelations(List<TaskRelation> taskRelations) {
        saveBatch(taskRelations);
    }

    @Override
    public List<TaskRelation> getTaskRelationsByParentTaskId(Long parentTaskId) {
        QueryWrapper<TaskRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_task_id", parentTaskId);
        return list(queryWrapper);
    }

    @Override
    public List<TaskRelation> getTaskRelationsByChildTaskId(Long childTaskId) {
        QueryWrapper<TaskRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("child_task_id", childTaskId);
        return list(queryWrapper);
    }

    @Override
    public TaskRelation getTaskRelation(Long parentTaskId, Long childTaskId) {
        QueryWrapper<TaskRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_task_id", parentTaskId);
        queryWrapper.eq("child_task_id", childTaskId);
        return getOne(queryWrapper);
    }

    @Override
    public boolean deleteTaskRelation(Long id) {
        return removeById(id);
    }

    @Override
    public int deleteTaskRelationsByParentTaskId(Long parentTaskId) {
        QueryWrapper<TaskRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_task_id", parentTaskId);
        return remove(queryWrapper) ? 1 : 0;
    }

    @Override
    public int deleteTaskRelationsByChildTaskId(Long childTaskId) {
        QueryWrapper<TaskRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("child_task_id", childTaskId);
        return remove(queryWrapper) ? 1 : 0;
    }
}
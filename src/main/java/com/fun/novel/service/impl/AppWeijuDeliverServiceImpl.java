package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.AppWeijuDeliver;
import com.fun.novel.mapper.AppWeijuDeliverMapper;
import com.fun.novel.service.AppWeijuDeliverService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppWeijuDeliverServiceImpl extends ServiceImpl<AppWeijuDeliverMapper, AppWeijuDeliver> implements AppWeijuDeliverService {
    
    @Override
    @Transactional
    public AppWeijuDeliver addDeliver(AppWeijuDeliver deliver) {
        save(deliver);
        return deliver;
    }

    @Override
    public List<AppWeijuDeliver> getDeliverList() {
        return list(new LambdaQueryWrapper<>());
    }

    @Override
    @Transactional
    public AppWeijuDeliver updateDeliver(AppWeijuDeliver deliver) {
        // 先检查记录是否存在
        AppWeijuDeliver existingDeliver = getDeliverByDeliverId(deliver.getDeliverId());
        if (existingDeliver == null) {
            throw new IllegalArgumentException("要更新的Deliver记录不存在");
        }
        
        // 设置主键ID
        deliver.setAdId(existingDeliver.getAdId());
        
        // 执行更新操作
        boolean updated = updateById(deliver);
        if (!updated) {
            throw new IllegalArgumentException("更新Deliver失败");
        }
        
        // 返回更新后的完整记录
        return getById(deliver.getAdId());
    }

    @Override
    public AppWeijuDeliver getDeliverByDeliverId(String deliverId) {
        LambdaQueryWrapper<AppWeijuDeliver> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppWeijuDeliver::getDeliverId, deliverId);
        return getOne(queryWrapper);
    }

    @Override
    @Transactional
    public boolean deleteByDeliverId(String deliverId) {
        LambdaQueryWrapper<AppWeijuDeliver> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppWeijuDeliver::getDeliverId, deliverId);
        return remove(queryWrapper);
    }
} 
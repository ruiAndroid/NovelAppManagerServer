package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.entity.AppWeijuDeliver;
import com.fun.novel.mapper.AppWeijuDeliverMapper;
import com.fun.novel.service.AppWeijuDeliverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppWeijuDeliverServiceImpl implements AppWeijuDeliverService {

    @Autowired
    private AppWeijuDeliverMapper deliverMapper;

    @Override
    @Transactional
    public AppWeijuDeliver addDeliver(AppWeijuDeliver deliver) {
        deliverMapper.insert(deliver);
        return deliver;
    }

    @Override
    @Transactional
    public AppWeijuDeliver updateDeliver(AppWeijuDeliver deliver) {
        deliverMapper.updateById(deliver);
        return deliver;
    }

    @Override
    @Transactional
    public void deleteDeliver(Integer id) {
        deliverMapper.deleteById(id);
    }

    @Override
    public AppWeijuDeliver getDeliverByDeliverId(String deliverId) {
        LambdaQueryWrapper<AppWeijuDeliver> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppWeijuDeliver::getDeliverId, deliverId);
        return deliverMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional
    public boolean deleteByDeliverId(String deliverId) {
        LambdaQueryWrapper<AppWeijuDeliver> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppWeijuDeliver::getDeliverId, deliverId);
        return deliverMapper.delete(queryWrapper) > 0;
    }
} 
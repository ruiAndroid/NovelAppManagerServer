package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.entity.NovelWeijuDeliver;
import com.fun.novel.mapper.NovelWeijuDeliverMapper;
import com.fun.novel.service.NovelWeijuDeliverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NovelWeijuDeliverServiceImpl implements NovelWeijuDeliverService {

    @Autowired
    private NovelWeijuDeliverMapper deliverMapper;

    @Override
    @Transactional
    public NovelWeijuDeliver addDeliver(NovelWeijuDeliver deliver) {
        deliverMapper.insert(deliver);
        return deliver;
    }

    @Override
    @Transactional
    public NovelWeijuDeliver updateDeliver(NovelWeijuDeliver deliver) {
        deliverMapper.updateById(deliver);
        return deliver;
    }

    @Override
    @Transactional
    public void deleteDeliver(Integer id) {
        deliverMapper.deleteById(id);
    }

    @Override
    public NovelWeijuDeliver getDeliverByDeliverId(String deliverId) {
        LambdaQueryWrapper<NovelWeijuDeliver> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelWeijuDeliver::getDeliverId, deliverId);
        return deliverMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional
    public boolean deleteByDeliverId(String deliverId) {
        LambdaQueryWrapper<NovelWeijuDeliver> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelWeijuDeliver::getDeliverId, deliverId);
        return deliverMapper.delete(queryWrapper) > 0;
    }
} 
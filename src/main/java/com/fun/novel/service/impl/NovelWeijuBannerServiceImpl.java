package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.entity.NovelWeijuBanner;
import com.fun.novel.mapper.NovelWeijuBannerMapper;
import com.fun.novel.service.NovelWeijuBannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NovelWeijuBannerServiceImpl implements NovelWeijuBannerService {
    
    @Autowired
    private NovelWeijuBannerMapper bannerMapper;

    @Override
    @Transactional
    public NovelWeijuBanner addBanner(NovelWeijuBanner banner) {
        bannerMapper.insert(banner);
        return banner;
    }

    @Override
    public List<NovelWeijuBanner> getBannerList() {
        return bannerMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    @Transactional
    public NovelWeijuBanner updateBanner(NovelWeijuBanner banner) {
        // 先检查记录是否存在
        NovelWeijuBanner existingBanner = bannerMapper.selectById(banner.getAdId());
        if (existingBanner == null) {
            throw new IllegalArgumentException("要更新的Banner记录不存在");
        }
        
        // 执行更新操作
        int rows = bannerMapper.updateById(banner);
        if (rows == 0) {
            throw new IllegalArgumentException("更新Banner失败");
        }
        
        // 返回更新后的完整记录
        return bannerMapper.selectById(banner.getAdId());
    }


    @Override
    public NovelWeijuBanner getBannerByBannerId(String bannerId) {
        LambdaQueryWrapper<NovelWeijuBanner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelWeijuBanner::getBannerId, bannerId);
        return bannerMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional
    public boolean deleteBannerByBannerId(String bannerId) {
        LambdaQueryWrapper<NovelWeijuBanner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelWeijuBanner::getBannerId, bannerId);
        return bannerMapper.delete(queryWrapper) > 0;
    }
} 
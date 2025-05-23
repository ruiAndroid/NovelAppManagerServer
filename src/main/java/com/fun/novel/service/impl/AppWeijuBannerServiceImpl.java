package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.AppWeijuBanner;
import com.fun.novel.mapper.AppWeijuBannerMapper;
import com.fun.novel.service.AppWeijuBannerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppWeijuBannerServiceImpl extends ServiceImpl<AppWeijuBannerMapper, AppWeijuBanner> implements AppWeijuBannerService {
    
    @Override
    @Transactional
    public AppWeijuBanner addBanner(AppWeijuBanner banner) {
        save(banner);
        return banner;
    }

    @Override
    public List<AppWeijuBanner> getBannerList() {
        return list(new LambdaQueryWrapper<>());
    }

    @Override
    @Transactional
    public AppWeijuBanner updateBanner(AppWeijuBanner banner) {
        // 先检查记录是否存在
        AppWeijuBanner existingBanner = getBannerByBannerId(banner.getBannerId());
        if (existingBanner == null) {
            throw new IllegalArgumentException("要更新的Banner记录不存在");
        }
        
        // 设置主键ID
        banner.setAdId(existingBanner.getAdId());
        
        // 执行更新操作
        boolean updated = updateById(banner);
        if (!updated) {
            throw new IllegalArgumentException("更新Banner失败");
        }
        
        // 返回更新后的完整记录
        return getById(banner.getAdId());
    }

    @Override
    public AppWeijuBanner getBannerByBannerId(String bannerId) {
        LambdaQueryWrapper<AppWeijuBanner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppWeijuBanner::getBannerId, bannerId);
        return getOne(queryWrapper);
    }

    @Override
    @Transactional
    public boolean deleteBannerByBannerId(String bannerId) {
        LambdaQueryWrapper<AppWeijuBanner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppWeijuBanner::getBannerId, bannerId);
        return remove(queryWrapper);
    }
} 
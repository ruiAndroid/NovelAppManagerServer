package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.AppUIConfig;
import com.fun.novel.mapper.AppUIConfigMapper;
import com.fun.novel.service.AppUIConfigService;
import org.springframework.stereotype.Service;

@Service
public class AppUIConfigServiceImpl extends ServiceImpl<AppUIConfigMapper, AppUIConfig> implements AppUIConfigService {

    @Override
    public AppUIConfig getByAppId(String appId) {
        LambdaQueryWrapper<AppUIConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUIConfig::getAppid, appId);
        return getOne(queryWrapper);
    }

    @Override
    public AppUIConfig createAppUIConfig(AppUIConfig appUIConfig) {
        // 检查是否已存在该appid的配置
        AppUIConfig existingConfig = getByAppId(appUIConfig.getAppid());
        if (existingConfig != null) {
            throw new IllegalArgumentException("该应用已存在UI配置");
        }
        
        // 保存新的UI配置
        save(appUIConfig);
        return appUIConfig;
    }
    
    @Override
    public AppUIConfig updateAppUIConfig(AppUIConfig appUIConfig) {
        // 检查是否存在该配置
        AppUIConfig existingConfig = getById(appUIConfig.getId());
        if (existingConfig == null) {
            throw new IllegalArgumentException("UI配置不存在");
        }
        
        // 检查appid是否被其他记录使用
        if (!existingConfig.getAppid().equals(appUIConfig.getAppid())) {
            AppUIConfig configWithSameAppId = getByAppId(appUIConfig.getAppid());
            if (configWithSameAppId != null && !configWithSameAppId.getId().equals(appUIConfig.getId())) {
                throw new IllegalArgumentException("该应用已存在UI配置");
            }
        }
        
        // 更新UI配置
        updateById(appUIConfig);
        return appUIConfig;
    }
    
    @Override
    public boolean deleteAppUIConfigByAppId(String appId) {
        // 检查是否存在该配置
        AppUIConfig existingConfig = getByAppId(appId);
        if (existingConfig == null) {
            throw new IllegalArgumentException("UI配置不存在");
        }
        
        // 删除UI配置
        LambdaQueryWrapper<AppUIConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUIConfig::getAppid, appId);
        return remove(queryWrapper);
    }
}
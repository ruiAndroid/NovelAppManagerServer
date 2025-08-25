package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.mapper.AppCommonConfigMapper;
import com.fun.novel.service.AppCommonConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppCommonConfigServiceImpl implements AppCommonConfigService {

    private final AppCommonConfigMapper appCommonConfigMapper;

    @Override
    public AppCommonConfig createAppCommonConfig(AppCommonConfigDTO dto) {
        // 检查是否已存在配置
        LambdaQueryWrapper<AppCommonConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppCommonConfig::getAppId, dto.getAppId());
        AppCommonConfig existingConfig = appCommonConfigMapper.selectOne(wrapper);
        log.info("Checking existing config for appId: {}, result: {}", dto.getAppId(), existingConfig);
        
        if (existingConfig != null) {
            throw new IllegalArgumentException("应用ID为 " + dto.getAppId() + " 的通用配置已存在，每个应用只能创建一条通用配置");
        }

        // 创建新配置
        AppCommonConfig config = new AppCommonConfig();
        BeanUtils.copyProperties(dto, config);

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        config.setCreateTime(now);
        config.setUpdateTime(now);
        
        appCommonConfigMapper.insert(config);
        return config;
    }

    @Override
    public AppCommonConfig getAppCommonConfig(String appid) {
        LambdaQueryWrapper<AppCommonConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppCommonConfig::getAppId, appid);
        return appCommonConfigMapper.selectOne(wrapper);
    }

    @Override
    public boolean deleteAppCommonConfig(String appId) {
        LambdaQueryWrapper<AppCommonConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppCommonConfig::getAppId, appId);
        return appCommonConfigMapper.delete(wrapper) > 0;
    }

    @Override
    public AppCommonConfig updateAppCommonConfig(AppCommonConfigDTO dto) {
        // 检查配置是否存在
        LambdaQueryWrapper<AppCommonConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppCommonConfig::getAppId, dto.getAppId());
        AppCommonConfig existingConfig = appCommonConfigMapper.selectOne(wrapper);
        
        if (existingConfig == null) {
            throw new IllegalArgumentException("应用ID为 " + dto.getAppId() + " 的通用配置不存在");
        }

        // 更新配置
        BeanUtils.copyProperties(dto, existingConfig);
        
        // 更新修改时间
        existingConfig.setUpdateTime(LocalDateTime.now());
        
        appCommonConfigMapper.updateById(existingConfig);
        return existingConfig;
    }
    
    @Override
    public List<AppCommonConfig> getAppCommonConfigByAppName(String appName) {
        // 通过novel_app表关联查询app_common_config表
        return appCommonConfigMapper.selectByAppName(appName);
    }
}
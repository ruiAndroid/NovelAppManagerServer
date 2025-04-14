package com.fun.novel.service.impl;

import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.mapper.AppCommonConfigMapper;
import com.fun.novel.service.AppCommonConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppCommonConfigServiceImpl implements AppCommonConfigService {

    private final AppCommonConfigMapper appCommonConfigMapper;

    @Override
    public boolean saveOrUpdateConfig(AppCommonConfigDTO dto) {
        AppCommonConfig config = new AppCommonConfig();
        BeanUtils.copyProperties(dto, config);
        
        AppCommonConfig existingConfig = appCommonConfigMapper.selectById(dto.getAppid());
        if (existingConfig == null) {
            return appCommonConfigMapper.insert(config) > 0;
        } else {
            return appCommonConfigMapper.updateById(config) > 0;
        }
    }

    @Override
    public AppCommonConfig getConfig(String appid) {
        return appCommonConfigMapper.selectById(appid);
    }
} 
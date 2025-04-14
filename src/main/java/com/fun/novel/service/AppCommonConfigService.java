package com.fun.novel.service;

import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.entity.AppCommonConfig;

public interface AppCommonConfigService {

    /**
     * 保存或更新应用配置
     * @param dto 配置信息
     * @return 保存结果
     */
    boolean saveOrUpdateConfig(AppCommonConfigDTO dto);

    /**
     * 获取应用配置
     * @param appid 应用ID
     * @return 配置信息
     */
    AppCommonConfig getConfig(String appid);
} 
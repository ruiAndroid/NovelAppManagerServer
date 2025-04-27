package com.fun.novel.service;

import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.entity.AppCommonConfig;

public interface AppCommonConfigService {

    /**
     * 创建应用通用配置
     * @param dto 配置信息
     * @return 创建后的配置实体
     */
    AppCommonConfig createAppCommonConfig(AppCommonConfigDTO dto);

    /**
     * 获取应用配置
     * @param appid 应用ID
     * @return 配置信息
     */
    AppCommonConfig getAppCommonConfig(String appid);

    /**
     * 删除应用配置
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteAppCommonConfig(String appId);

    /**
     * 更新应用配置
     * @param dto 配置信息
     * @return 更新后的配置实体
     */
    AppCommonConfig updateAppCommonConfig(AppCommonConfigDTO dto);
} 
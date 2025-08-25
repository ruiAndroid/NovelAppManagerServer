package com.fun.novel.service;

import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.entity.AppCommonConfig;

import java.util.List;

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
    
    /**
     * 根据应用名称获取应用配置列表
     * @param appName 应用名称
     * @return 配置信息列表
     */
    List<AppCommonConfig> getAppCommonConfigByAppName(String appName);
}
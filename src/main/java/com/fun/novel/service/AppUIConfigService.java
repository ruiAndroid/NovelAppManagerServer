package com.fun.novel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fun.novel.entity.AppUIConfig;

import java.util.List;

public interface AppUIConfigService extends IService<AppUIConfig> {
    
    /**
     * 根据appid查询UI配置
     * @param appid 应用ID
     * @return UI配置信息
     */
    AppUIConfig getByAppId(String appid);
    
    /**
     * 根据appid列表批量查询UI配置
     * @param appIds 应用ID列表
     * @return UI配置信息列表
     */
    List<AppUIConfig> getByAppIds(List<String> appIds);
    
    /**
     * 创建UI配置
     * @param appUIConfig UI配置对象
     * @return 创建后的UI配置对象
     */
    AppUIConfig createAppUIConfig(AppUIConfig appUIConfig);
    
    /**
     * 更新UI配置
     * @param appUIConfig UI配置对象
     * @return 更新后的UI配置对象
     */
    AppUIConfig updateAppUIConfig(AppUIConfig appUIConfig);
    
    /**
     * 根据appid删除UI配置
     * @param appid 应用ID
     * @return 是否删除成功
     */
    boolean deleteAppUIConfigByAppId(String appid);
}
package com.fun.novel.service;

import com.fun.novel.entity.NovelApp;

import java.util.List;
import java.util.Map;

public interface NovelAppService {
    /**
     * 添加小说应用
     * @param novelApp 小说应用信息
     * @return 添加后的应用对象
     * @throws IllegalArgumentException 当 appid 已存在时抛出
     */
    NovelApp addNovelApp(NovelApp novelApp) throws IllegalArgumentException;

    /**
     * 获取按平台分组的小说应用列表
     * @return 按平台分组的小说应用Map
     */
    Map<String, List<NovelApp>> getNovelAppsByPlatform();

    /**
     * 根据appId删除小说应用
     * @param appId 应用ID
     * @return 删除结果
     * @throws IllegalArgumentException 当应用不存在时抛出
     */
    boolean deleteByAppId(String appId) throws IllegalArgumentException;

    /**
     * 更新小说应用
     * @param novelApp 小说应用信息
     * @return 更新后的应用对象
     * @throws IllegalArgumentException 当应用不存在时抛出
     */
    NovelApp updateNovelApp(NovelApp novelApp);

    /**
     * 根据应用ID获取应用信息
     * @param appId 应用ID
     * @return 应用信息
     */
    NovelApp getByAppId(String appId);
    
    /**
     * 根据应用名称获取所有平台的应用信息
     * @param appName 应用名称
     * @return 应用信息列表
     */
    List<NovelApp> getAppsByAppName(String appName);

    NovelApp getAppsByNameAndPlatform(String appName,String platform);
}
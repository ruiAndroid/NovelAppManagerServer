package com.fun.novel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fun.novel.ai.entity.FunAiApp;

import java.util.List;

/**
 * AI应用服务接口
 */
public interface FunAiAppService extends IService<FunAiApp> {
    /**
     * 根据用户ID查询应用列表
     * @param userId 用户ID
     * @return 应用列表
     */
    List<FunAiApp> getAppsByUserId(Long userId);

    /**
     * 根据用户ID分页查询应用列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页条数
     * @return 分页应用列表
     */
    Page<FunAiApp> getAppsByUserId(Long userId, Integer page, Integer size);

    /**
     * 根据应用ID和用户ID查询应用
     * @param appId 应用ID
     * @param userId 用户ID
     * @return 应用信息
     */
    FunAiApp getAppByIdAndUserId(Long appId, Long userId);

    /**
     * 创建应用
     * @param app 应用信息
     * @return 创建后的应用信息
     */
    FunAiApp createApp(FunAiApp app);

    /**
     * 更新应用
     * @param app 应用信息
     * @param userId 用户ID
     * @return 更新后的应用信息
     */
    FunAiApp updateApp(FunAiApp app, Long userId);

    /**
     * 删除应用
     * @param appId 应用ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteApp(Long appId, Long userId);
}

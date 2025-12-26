package com.fun.novel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fun.novel.ai.entity.FunAiApp;
import com.fun.novel.dto.FunAiAppDeployResponse;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 创建应用（包含完整的业务逻辑：校验、创建文件夹、更新用户计数等）
     * @param userId 用户ID
     * @return 创建后的应用信息
     * @throws IllegalArgumentException 当用户不存在或应用数量已达上限时抛出
     */
    FunAiApp createAppWithValidation(Long userId) throws IllegalArgumentException;

    /**
     * 上传应用文件（zip压缩包）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param file 上传的zip文件
     * @return 保存的文件路径
     * @throws IllegalArgumentException 当文件格式不正确、应用不存在或无权限时抛出
     */
    String uploadAppFile(Long userId, Long appId, MultipartFile file) throws IllegalArgumentException;

    /**
     * 修改应用基础信息（appName/appDescription/appType）
     * - appName：同一用户下不可重名（排除当前 appId）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param appName 应用名称
     * @param appDescription 应用描述
     * @param appType 应用类型
     * @return 更新后的应用信息
     * @throws IllegalArgumentException 当应用不存在、无权限或 appName 重名/非法时抛出
     */
    FunAiApp updateBasicInfo(Long userId, Long appId, String appName, String appDescription, String appType)
            throws IllegalArgumentException;

    /**
     * 部署应用（基于用户目录中的 zip 包进行解压 + 异步 npm install/build）
     * 状态机：
     * - 仅当 appStatus == 1（UPLOADED，已上传）才允许部署
     * - 接口返回时：置为 2（DEPLOYING，部署中）
     * - 异步构建成功：置为 3（READY，可访问）
     * - 异步构建失败：置为 4（FAILED，部署失败）并写 lastDeployError
     */
    FunAiAppDeployResponse deployApp(Long userId, Long appId) throws IllegalArgumentException;
}

package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.ai.entity.FunAiApp;
import com.fun.novel.ai.entity.FunAiUser;
import com.fun.novel.mapper.FunAiAppMapper;
import com.fun.novel.service.FunAiAppService;
import com.fun.novel.service.FunAiUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * AI应用服务实现类
 */
@Service
public class FunAiAppServiceImpl extends ServiceImpl<FunAiAppMapper, FunAiApp> implements FunAiAppService {

    private static final Logger logger = LoggerFactory.getLogger(FunAiAppServiceImpl.class);

    @Autowired
    private FunAiUserService funAiUserService;

    @Value("${funai.userPath}")
    private String userPath;

    @Override
    public List<FunAiApp> getAppsByUserId(Long userId) {
        QueryWrapper<FunAiApp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }


    @Override
    public FunAiApp getAppByIdAndUserId(Long appId, Long userId) {
        QueryWrapper<FunAiApp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", appId)
                .eq("user_id", userId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public FunAiApp createApp(FunAiApp app) {
        // 生成应用密钥
        if (!StringUtils.hasText(app.getAppKey())) {
            app.setAppKey(generateAppKey());
        }
        if (!StringUtils.hasText(app.getAppSecret())) {
            app.setAppSecret(generateAppSecret());
        }
        // 设置默认状态为启用
        if (app.getAppStatus() == null) {
            app.setAppStatus(1);
        }
        // 保存应用
        save(app);
        return app;
    }

    @Override
    public FunAiApp updateApp(FunAiApp app, Long userId) {
        // 验证应用是否属于该用户
        FunAiApp existingApp = getAppByIdAndUserId(app.getId(), userId);
        if (existingApp == null) {
            throw new RuntimeException("应用不存在或无权限操作");
        }
        // 更新应用信息
        app.setUserId(userId); // 确保用户ID不变
        updateById(app);
        return app;
    }

    @Override
    public boolean deleteApp(Long appId, Long userId) {
        // 验证应用是否属于该用户
        FunAiApp existingApp = getAppByIdAndUserId(appId, userId);
        if (existingApp == null) {
            throw new RuntimeException("应用不存在或无权限操作");
        }
        
        // 校验应用状态：如果状态为2（运行中），则不允许删除
        if (existingApp.getAppStatus() != null && existingApp.getAppStatus() == 2) {
            logger.warn("尝试删除运行中的应用: appId={}, userId={}, appStatus={}", 
                appId, userId, existingApp.getAppStatus());
            throw new IllegalArgumentException("应用正在运行中，需要先停止");
        }
        
        // 删除应用文件夹
        try {
            deleteAppFolder(userId, existingApp.getAppName());
        } catch (Exception e) {
            logger.error("删除应用文件夹失败: appId={}, userId={}, appName={}, error={}", 
                appId, userId, existingApp.getAppName(), e.getMessage(), e);
            // 文件夹删除失败不影响数据库删除，记录日志即可
        }
        
        // 删除数据库记录
        boolean deleted = removeById(appId);
        
        // 如果删除成功，更新用户的 app_count
        if (deleted) {
            try {
                updateUserAppCountAfterDelete(userId);
            } catch (Exception e) {
                logger.error("更新用户应用数量失败: userId={}, error={}", userId, e.getMessage(), e);
                // 更新失败不影响删除结果，记录日志即可
            }
        }
        
        return deleted;
    }

    /**
     * 生成应用密钥
     * @return 应用密钥
     */
    private String generateAppKey() {
        return "AIK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    /**
     * 生成应用密钥
     * @return 应用密钥
     */
    private String generateAppSecret() {
        return "AIS_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public FunAiApp createAppWithValidation(Long userId) throws IllegalArgumentException {
        // 0. 校验用户应用数量是否已达上限
        FunAiUser user = funAiUserService.getById(userId);
        if (user == null) {
            logger.error("用户不存在: userId={}", userId);
            throw new IllegalArgumentException("用户不存在");
        }
        
        // 检查 app_count，如果为 null 则视为 0
        Integer appCount = user.getAppCount();
        if (appCount == null) {
            appCount = 0;
        }
        
        if (appCount >= 20) {
            logger.warn("用户应用数量已达上限: userId={}, appCount={}", userId, appCount);
            throw new IllegalArgumentException("已达项目数量上限");
        }
        
        // 1. 查询用户实际的应用列表（用于生成应用名称）
        List<FunAiApp> existingApps = getAppsByUserId(userId);
        
        // 2. 生成应用名称：找到下一个可用的"未命名应用X"
        String appName = generateNextAppName(existingApps);
        
        // 3. 创建用户专属文件夹（如果不存在）
        String basePath = getUserPath();
        if (basePath == null || basePath.isEmpty()) {
            logger.error("用户路径配置为空");
            throw new IllegalArgumentException("用户路径配置为空");
        }
        String userDirPath = basePath + File.separator + userId;
        Path userDir = Paths.get(userDirPath);
        try {
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
                logger.info("创建用户专属文件夹: {}", userDirPath);
            }
        } catch (java.io.IOException e) {
            logger.error("创建用户专属文件夹失败: {}", userDirPath, e);
            throw new RuntimeException("创建用户专属文件夹失败: " + e.getMessage(), e);
        }

        // 4. 创建FunAiApp对象，设置默认值
        FunAiApp app = new FunAiApp();
        app.setUserId(userId);
        app.setAppName(appName);
        app.setAppDescription("这是一个默认应用");
        app.setAppType("default");
        app.setAppStatus(1); // 默认启用

        // 5. 保存到数据库（会自动生成id）
        FunAiApp createdApp = createApp(app);

        // 6. 使用应用名称创建应用文件夹（处理特殊字符）
        try {
            // 将应用名称中的特殊字符替换为下划线，确保文件夹名称合法
            String appDirName = sanitizeFileName(appName);
            String appDirPath = userDirPath + File.separator + appDirName;
            Path appDir = Paths.get(appDirPath);
            Files.createDirectories(appDir);
            logger.info("创建应用文件夹: {}", appDirPath);
        } catch (java.io.IOException folderException) {
            // 如果创建文件夹失败，回滚数据库操作
            logger.error("创建应用文件夹失败，回滚数据库记录: appId={}, error={}", 
                createdApp.getId(), folderException.getMessage(), folderException);
            try {
                removeById(createdApp.getId());
                logger.info("已删除数据库记录: appId={}", createdApp.getId());
            } catch (Exception rollbackException) {
                logger.error("回滚数据库记录失败: appId={}, error={}", 
                    createdApp.getId(), rollbackException.getMessage(), rollbackException);
            }
            throw new RuntimeException("创建应用文件夹失败: " + folderException.getMessage(), folderException);
        }
        
        // 7. 更新用户的 app_count
        try {
            Integer newAppCount = appCount + 1;
            user.setAppCount(newAppCount);
            funAiUserService.updateById(user);
            logger.info("更新用户应用数量: userId={}, newAppCount={}", userId, newAppCount);
        } catch (Exception updateException) {
            // 如果更新失败，记录日志但不影响创建结果
            logger.error("更新用户应用数量失败: userId={}, error={}", 
                userId, updateException.getMessage(), updateException);
        }
        
        return createdApp;
    }

    /**
     * 获取处理后的用户路径（去除引号和空格）
     */
    private String getUserPath() {
        if (userPath == null) {
            return null;
        }
        // 去除首尾的引号和空格
        return userPath.trim().replaceAll("^[\"']|[\"']$", "");
    }

    /**
     * 生成下一个可用的应用名称
     * @param existingApps 用户现有的应用列表
     * @return 新的应用名称，格式为"未命名应用X"
     */
    private String generateNextAppName(List<FunAiApp> existingApps) {
        if (existingApps == null || existingApps.isEmpty()) {
            return "未命名应用1";
        }
        
        // 收集所有已使用的数字
        Set<Integer> usedNumbers = new HashSet<>();
        for (FunAiApp app : existingApps) {
            String name = app.getAppName();
            if (name != null && name.startsWith("未命名应用")) {
                try {
                    String numberStr = name.substring("未命名应用".length());
                    int number = Integer.parseInt(numberStr);
                    usedNumbers.add(number);
                } catch (NumberFormatException e) {
                    // 如果不是纯数字，忽略
                }
            }
        }
        
        // 找到第一个未使用的数字
        int nextNumber = 1;
        while (usedNumbers.contains(nextNumber)) {
            nextNumber++;
        }
        
        return "未命名应用" + nextNumber;
    }

    /**
     * 清理文件名，将特殊字符替换为下划线，确保文件夹名称合法
     * @param fileName 原始文件名
     * @return 清理后的文件名
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unnamed";
        }
        // 替换 Windows 和 Linux 文件系统不支持的字符
        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_");
    }

    /**
     * 删除应用文件夹
     * @param userId 用户ID
     * @param appName 应用名称
     * @throws java.io.IOException IO异常
     */
    private void deleteAppFolder(Long userId, String appName) throws java.io.IOException {
        String basePath = getUserPath();
        if (basePath == null || basePath.isEmpty()) {
            logger.warn("用户路径配置为空，跳过删除应用文件夹");
            return;
        }
        
        // 构建应用文件夹路径：basePath/userId/sanitizeFileName(appName)
        String userDirPath = basePath + File.separator + userId;
        String appDirName = sanitizeFileName(appName);
        String appDirPath = userDirPath + File.separator + appDirName;
        Path appDir = Paths.get(appDirPath);
        
        // 如果文件夹不存在，直接返回
        if (!Files.exists(appDir)) {
            logger.info("应用文件夹不存在，跳过删除: {}", appDirPath);
            return;
        }
        
        // 递归删除文件夹
        deleteDirectoryRecursively(appDir);
        logger.info("成功删除应用文件夹: {}", appDirPath);
    }

    /**
     * 递归删除目录及其所有内容
     * @param path 目录路径
     * @throws java.io.IOException IO异常
     */
    private void deleteDirectoryRecursively(Path path) throws java.io.IOException {
        if (Files.notExists(path)) {
            return;
        }
        Files.walk(path)
                .sorted((p1, p2) -> p2.toString().compareTo(p1.toString())) // 逆序排列，先删除文件再删除目录
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (java.io.IOException e) {
                        logger.warn("删除文件/目录失败: {}, error={}", p, e.getMessage());
                    }
                });
    }

    /**
     * 删除应用后更新用户的 app_count
     * @param userId 用户ID
     */
    private void updateUserAppCountAfterDelete(Long userId) {
        FunAiUser user = funAiUserService.getById(userId);
        if (user == null) {
            logger.warn("用户不存在，跳过更新应用数量: userId={}", userId);
            return;
        }
        
        Integer appCount = user.getAppCount();
        if (appCount == null || appCount <= 0) {
            appCount = 0;
        } else {
            appCount = appCount - 1;
        }
        
        user.setAppCount(appCount);
        funAiUserService.updateById(user);
        logger.info("更新用户应用数量: userId={}, newAppCount={}", userId, appCount);
    }
}

package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.ai.entity.FunAiApp;
import com.fun.novel.ai.entity.FunAiUser;
import com.fun.novel.ai.enums.FunAiAppStatus;
import com.fun.novel.dto.FunAiAppDeployResponse;
import com.fun.novel.mapper.FunAiAppMapper;
import com.fun.novel.service.FunAiAppService;
import com.fun.novel.service.FunAiUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    private final ExecutorService deployExecutor = Executors.newCachedThreadPool();

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
        // 设置默认状态为：空壳/草稿（仅创建记录）
        if (app.getAppStatus() == null) {
            app.setAppStatus(FunAiAppStatus.CREATED.code());
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
    public FunAiApp updateBasicInfo(Long userId, Long appId, String appName, String appDescription, String appType)
            throws IllegalArgumentException {
        if (userId == null || appId == null) {
            throw new IllegalArgumentException("userId/appId 不能为空");
        }

        // 1) 校验应用归属
        FunAiApp existingApp = getAppByIdAndUserId(appId, userId);
        if (existingApp == null) {
            throw new IllegalArgumentException("应用不存在或无权限操作");
        }

        // 部署中不允许修改基础信息（避免目录/部署过程冲突）
        if (existingApp.getAppStatus() != null && existingApp.getAppStatus() == FunAiAppStatus.DEPLOYING.code()) {
            throw new IllegalArgumentException("应用部署中，暂不允许修改");
        }

        // 2) appName 同名校验（仅当传入且非空时才校验/更新），并同步重命名本地目录
        if (StringUtils.hasText(appName)) {
            String trimmedName = appName.trim();
            QueryWrapper<FunAiApp> nameCheck = new QueryWrapper<>();
            nameCheck.eq("user_id", userId)
                    .eq("app_name", trimmedName)
                    .ne("id", appId)
                    .last("limit 1");
            FunAiApp duplicate = baseMapper.selectOne(nameCheck);
            if (duplicate != null) {
                throw new IllegalArgumentException("应用名称已存在，请换一个名称");
            }
            // 如果名称变化，尝试重命名应用目录（目录名目前依赖 appName）
            String oldName = existingApp.getAppName();
            if (oldName != null && !oldName.equals(trimmedName)) {
                String basePath = getUserPath();
                if (basePath != null && !basePath.isEmpty()) {
                    Path userDir = Paths.get(basePath, String.valueOf(userId));
                    Path oldDir = userDir.resolve(sanitizeFileName(oldName));
                    Path newDir = userDir.resolve(sanitizeFileName(trimmedName));
                    try {
                        if (Files.exists(oldDir) && Files.notExists(newDir)) {
                            Files.move(oldDir, newDir);
                            logger.info("应用目录重命名成功: {} -> {}", oldDir, newDir);
                        }
                    } catch (Exception e) {
                        logger.error("应用目录重命名失败: {} -> {}, error={}", oldDir, newDir, e.getMessage(), e);
                        throw new IllegalArgumentException("应用目录重命名失败，请稍后重试");
                    }
                }
            }
            existingApp.setAppName(trimmedName);
        }

        // 3) 其他字段更新（允许置空：如果你不想允许置空，可以改成 hasText 才更新）
        if (appDescription != null) {
            existingApp.setAppDescription(appDescription);
        }
        if (appType != null) {
            existingApp.setAppType(appType);
        }

        updateById(existingApp);
        return existingApp;
    }

    @Override
    public FunAiAppDeployResponse deployApp(Long userId, Long appId) throws IllegalArgumentException {
        if (userId == null || appId == null) {
            throw new IllegalArgumentException("userId/appId 不能为空");
        }

        FunAiApp app = getAppByIdAndUserId(appId, userId);
        if (app == null) {
            throw new IllegalArgumentException("应用不存在或无权限操作");
        }

        // 仅当 UPLOADED 时允许部署
        if (app.getAppStatus() == null || app.getAppStatus() != FunAiAppStatus.UPLOADED.code()) {
            throw new IllegalArgumentException("当前应用状态不允许部署（请先上传zip）");
        }

        String basePath = getUserPath();
        if (basePath == null || basePath.isEmpty()) {
            throw new IllegalArgumentException("用户路径配置为空");
        }

        String userDirPath = basePath + File.separator + userId;
        String appDirName = sanitizeFileName(app.getAppName());
        Path appDir = Paths.get(userDirPath, appDirName);
        if (Files.notExists(appDir)) {
            throw new IllegalArgumentException("应用目录不存在，请先创建应用或重新上传");
        }

        // 1) 找到最新上传的 zip（兼容 upload_*.zip 或任意 .zip）
        Path zipPath;
        try {
            zipPath = Files.list(appDir)
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }))
                    .orElse(null);
        } catch (IOException e) {
            logger.error("读取应用目录失败: {}", appDir, e);
            throw new RuntimeException("读取应用目录失败: " + e.getMessage(), e);
        }

        if (zipPath == null || Files.notExists(zipPath)) {
            throw new IllegalArgumentException("未找到已上传的zip包，请先上传");
        }

        // 2) 更新状态为部署中（DEPLOYING），并清空上次失败原因，避免并发部署
        app.setAppStatus(FunAiAppStatus.DEPLOYING.code());
        app.setLastDeployError(null);
        updateById(app);

        Path deployDir = appDir.resolve("deploy");
        Path projectRoot;
        try {
            // 3) 解压到 deploy 目录（每次部署覆盖旧目录）
            if (Files.exists(deployDir)) {
                deleteDirectoryRecursively(deployDir);
            }
            Files.createDirectories(deployDir);
            unzipSafely(zipPath, deployDir);

            // 4) 尝试识别项目根目录（兼容 zip 内带一层顶级目录）
            projectRoot = detectProjectRoot(deployDir);

            // 5) 校验：必须是前端项目（至少要有 package.json）
            if (Files.notExists(projectRoot.resolve("package.json"))) {
                throw new IllegalArgumentException("zip内容不是有效的前端项目（缺少package.json）");
            }
        } catch (IllegalArgumentException e) {
            // 业务校验失败：进入 FAILED，方便用户修复后重试
            app.setAppStatus(FunAiAppStatus.FAILED.code());
            app.setLastDeployError(truncateError(e.getMessage()));
            updateById(app);
            throw e;
        } catch (Exception e) {
            logger.error("部署解压失败: userId={}, appId={}, error={}", userId, appId, e.getMessage(), e);
            app.setAppStatus(FunAiAppStatus.FAILED.code());
            app.setLastDeployError(truncateError(e.getMessage()));
            updateById(app);
            throw new RuntimeException("部署解压失败: " + e.getMessage(), e);
        }

        // 6) 异步执行 npm install && npm run build（不阻塞接口返回；前端轮询 appInfo 查看状态）
        final Path finalProjectRoot = projectRoot;
        deployExecutor.submit(() -> {
            try {
                // 再次确认当前状态仍为 DEPLOYING，避免重复/并发部署导致状态错乱
                FunAiApp latest = getAppByIdAndUserId(appId, userId);
                if (latest == null || latest.getAppStatus() == null || latest.getAppStatus() != FunAiAppStatus.DEPLOYING.code()) {
                    logger.info("跳过构建：应用状态已变化: userId={}, appId={}, appStatus={}",
                            userId, appId, latest == null ? null : latest.getAppStatus());
                    return;
                }

                executeCommandNoLog(finalProjectRoot, "npm install");
                // 使用相对 base，避免 build 后资源引用变成 /assets/* 导致站点在子路径下无法访问
                // 对于 Vite：--base=./ 会让 index.html 使用相对资源路径（如 assets/xxx.js）
                executeCommandNoLog(finalProjectRoot, "npm run build -- --base=./");

                // build 成功：进入 READY（dist 已生成，可访问）
                latest.setAppStatus(FunAiAppStatus.READY.code());
                latest.setLastDeployError(null);
                updateById(latest);
                logger.info("部署构建完成: userId={}, appId={}, status=READY", userId, appId);
            } catch (Exception e) {
                logger.error("部署构建失败: userId={}, appId={}, error={}", userId, appId, e.getMessage(), e);
                try {
                    FunAiApp latest = getAppByIdAndUserId(appId, userId);
                    if (latest != null) {
                        latest.setAppStatus(FunAiAppStatus.FAILED.code());
                        latest.setLastDeployError(truncateError(e.getMessage()));
                        updateById(latest);
                    }
                } catch (Exception ignore) {
                }
            }
        });

        FunAiAppDeployResponse resp = new FunAiAppDeployResponse();
        resp.setAppId(appId);
        resp.setUserId(userId);
        resp.setAppName(app.getAppName());
        resp.setAppStatus(app.getAppStatus());
        resp.setZipFileName(zipPath.getFileName().toString());
        resp.setProjectPath(projectRoot.toString());
        return resp;
    }

    @PreDestroy
    public void shutdownDeployExecutor() {
        deployExecutor.shutdown();
    }

    /**
     * 执行命令（不输出日志；失败抛出异常）
     */
    private void executeCommandNoLog(Path workDir, String cmd) throws IOException, InterruptedException {
        if (workDir == null || Files.notExists(workDir)) {
            throw new IOException("工作目录不存在: " + workDir);
        }
        if (cmd == null || cmd.isBlank()) {
            throw new IllegalArgumentException("命令不能为空");
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            // Windows: 设置 codepage 避免中文乱码；不输出到前端，但仍需读取避免阻塞
            processBuilder.command("cmd.exe", "/c", "chcp 65001>nul && " + cmd);
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        } else {
            processBuilder.command("sh", "-c", cmd);
        }
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 只保留最近一部分输出用于报错排查（避免占用过多内存）
                if (output.length() < 8000) {
                    output.append(line).append('\n');
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("命令执行失败，exitCode=" + exitCode + "，cmd=" + cmd + "，output=" + output);
        }
    }

    private String truncateError(String msg) {
        if (msg == null) {
            return null;
        }
        String m = msg.trim();
        if (m.length() <= 2000) {
            return m;
        }
        return m.substring(0, 2000);
    }

    /**
     * 安全解压：防止 Zip Slip（路径穿越）
     */
    private void unzipSafely(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName() == null || entry.getName().isBlank()) {
                    continue;
                }
                Path newPath = destDir.resolve(entry.getName()).normalize();
                if (!newPath.startsWith(destDir)) {
                    throw new IOException("非法zip条目路径: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 识别 react+vite 项目根目录：优先返回包含 package.json 的目录
     * - 如果 deployDir 下就有 package.json：返回 deployDir
     * - 如果 deployDir 下只有一个子目录且该子目录包含 package.json：返回该子目录
     */
    private Path detectProjectRoot(Path deployDir) {
        try {
            if (Files.exists(deployDir.resolve("package.json"))) {
                return deployDir;
            }
            List<Path> children = Files.list(deployDir)
                    .filter(Files::isDirectory)
                    .toList();
            if (children.size() == 1) {
                Path only = children.get(0);
                if (Files.exists(only.resolve("package.json"))) {
                    return only;
                }
            }
        } catch (IOException ignore) {
        }
        return deployDir;
    }

    @Override
    public boolean deleteApp(Long appId, Long userId) {
        // 验证应用是否属于该用户
        FunAiApp existingApp = getAppByIdAndUserId(appId, userId);
        if (existingApp == null) {
            throw new RuntimeException("应用不存在或无权限操作");
        }
        
        // DEPLOYING 或 DISABLED 不允许删除
        Integer st = existingApp.getAppStatus();
        if (st != null && (st == FunAiAppStatus.DEPLOYING.code() || st == FunAiAppStatus.DISABLED.code())) {
            logger.warn("尝试删除不可删除状态的应用: appId={}, userId={}, appStatus={}",
                    appId, userId, st);
            throw new IllegalArgumentException("应用部署中或已禁用，暂不允许删除");
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
        app.setAppStatus(FunAiAppStatus.CREATED.code()); // 默认空壳

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

    @Override
    public String uploadAppFile(Long userId, Long appId, MultipartFile file) throws IllegalArgumentException {
        // 1. 验证文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        // 2. 验证文件格式是否为 zip
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("只能上传 zip 格式的文件");
        }

        // 3. 验证应用是否存在且属于该用户
        FunAiApp app = getAppByIdAndUserId(appId, userId);
        if (app == null) {
            throw new IllegalArgumentException("应用不存在或无权限操作");
        }
        // 部署中不允许上传（避免替换构建中的资源）
        if (app.getAppStatus() != null && app.getAppStatus() == FunAiAppStatus.DEPLOYING.code()) {
            throw new IllegalArgumentException("应用部署中，暂不允许上传");
        }

        // 4. 获取应用文件夹路径
        String basePath = getUserPath();
        if (basePath == null || basePath.isEmpty()) {
            logger.error("用户路径配置为空");
            throw new IllegalArgumentException("用户路径配置为空");
        }

        String userDirPath = basePath + File.separator + userId;
        String appDirName = sanitizeFileName(app.getAppName());
        String appDirPath = userDirPath + File.separator + appDirName;
        Path appDir = Paths.get(appDirPath);

        // 5. 确保应用文件夹存在
        try {
            if (!Files.exists(appDir)) {
                Files.createDirectories(appDir);
                logger.info("创建应用文件夹: {}", appDirPath);
            }
        } catch (IOException e) {
            logger.error("创建应用文件夹失败: {}", appDirPath, e);
            throw new RuntimeException("创建应用文件夹失败: " + e.getMessage(), e);
        }

        // 6. 生成保存的文件名（使用时间戳避免覆盖）
        String timestamp = String.valueOf(System.currentTimeMillis());
        String savedFileName = "upload_" + timestamp + "_" + originalFilename;
        Path targetPath = appDir.resolve(savedFileName);

        // 7. 保存文件
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("文件上传成功: userId={}, appId={}, filePath={}", userId, appId, targetPath);
        } catch (IOException e) {
            logger.error("文件保存失败: userId={}, appId={}, error={}", userId, appId, e.getMessage(), e);
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }

        // 8. 仅保留最新 3 个 zip，避免占用过多磁盘
        try {
            List<Path> zips = Files.list(appDir)
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();

            for (int i = 3; i < zips.size(); i++) {
                try {
                    Files.deleteIfExists(zips.get(i));
                    logger.info("清理旧zip: {}", zips.get(i));
                } catch (Exception deleteEx) {
                    logger.warn("清理旧zip失败: {}, error={}", zips.get(i), deleteEx.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("清理旧zip异常（不影响上传结果）: userId={}, appId={}, error={}", userId, appId, e.getMessage());
        }

        // 9. 更新状态：UPLOADED，并清空上次部署错误
        try {
            app.setAppStatus(FunAiAppStatus.UPLOADED.code());
            app.setLastDeployError(null);
            updateById(app);
        } catch (Exception e) {
            logger.warn("上传成功但更新应用状态失败: userId={}, appId={}, error={}", userId, appId, e.getMessage());
        }

        // 10. 返回保存的文件路径（相对路径）
        return appDirPath + File.separator + savedFileName;
    }
}

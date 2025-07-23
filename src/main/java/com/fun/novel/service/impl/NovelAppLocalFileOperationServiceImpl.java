package com.fun.novel.service.impl;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppLocalFileOperationService;
import com.fun.novel.utils.CreateNovelTaskLogger;
import com.fun.novel.dto.CreateNovelLogType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Service
public class NovelAppLocalFileOperationServiceImpl implements NovelAppLocalFileOperationService {
    @Autowired
    private CreateNovelTaskLogger taskLogger;
    @Value("${build.workPath}")
    private String buildWorkPath;

    private static final int FILE_STEP_DELAY_MS = 1000;

    @Override
    public void processLocalCodeFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        doProcessLocalCodeFiles(taskId, params, rollbackActions, true);
    }

    public void processLocalCodeFilesSimple(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        doProcessLocalCodeFiles(null, params, rollbackActions, false);
    }

    private void doProcessLocalCodeFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        processPrebuildBuildDir(taskId, buildCode, platform, baseConfig, rollbackActions, withLogAndDelay);
        processThemeFile(taskId, buildCode, baseConfig, rollbackActions, withLogAndDelay);
        processDouyinPrefetchFile(taskId, buildCode, platform, rollbackActions, withLogAndDelay);
        processBaseConfigFile(taskId, buildCode, platform, baseConfig, commonConfig, rollbackActions, withLogAndDelay);
        processAdConfigFile(taskId, buildCode, platform, params.getAdConfig(), rollbackActions, withLogAndDelay);
        processPayConfigFile(taskId, buildCode, platform, params.getPaymentConfig(), rollbackActions, withLogAndDelay);
        processDeliverConfigFile(taskId, buildCode, platform, params.getDeliverConfig(), rollbackActions, withLogAndDelay);
        processCommonConfigFile(taskId, buildCode, platform, commonConfig, rollbackActions, withLogAndDelay);
        processAppConfigFile(taskId, buildCode, rollbackActions, withLogAndDelay);
        processPackageJsonFile(taskId, buildCode, platform, rollbackActions, withLogAndDelay);
    }

    @Override
    public void updateBaseConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        
        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        //修改基础设置
        processBaseConfigFile(null, buildCode, platform, baseConfig, commonConfig, rollbackActions, false);
        //修改主题颜色
        processThemeFile(null, buildCode, baseConfig, rollbackActions, false);
        //处理preBuild
        processPrebuildBuildDir(null, buildCode,platform, baseConfig, rollbackActions, false);
        //修改deliver 配置
        processDeliverConfigFile(null, buildCode, platform,  params.getDeliverConfig(), rollbackActions, false);

    }

    @Override
    public void updateCommonConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        //修改通用配置
        processCommonConfigFile(null,buildCode,platform, commonConfig, rollbackActions, false);

    }

    @Override
    public void updateAdConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        processAdConfigFile(null, buildCode, platform, params.getAdConfig(), rollbackActions, false);
    }

    @Override
    public void updatePayConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        processPayConfigFile(null, buildCode, platform, params.getPaymentConfig(), rollbackActions, false);
    }


    // 以下为迁移自NovelAppCreationServiceImpl的所有私有方法和工具方法
    // 1. processPrebuildBuildDir
    private void processPrebuildBuildDir(String taskId, String buildCode, String platform, CreateNovelAppRequest.BaseConfig baseConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-1-1] 处理目录prebuild/build: ", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String prebuildDir = buildWorkPath + File.separator + "prebuild" + File.separator + "build";
        String srcDir = prebuildDir + File.separator + "sample";
        String destDir = prebuildDir + File.separator + buildCode;
        try {
            Path destPath = java.nio.file.Paths.get(destDir);
            if (java.nio.file.Files.exists(destPath)) {
                deleteDirectoryRecursively(destPath);
            }
            java.nio.file.Files.createDirectories(destPath);
            // 立即添加回滚动作，确保后续任何失败都能回滚buildCode目录
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：删除" + destDir, CreateNovelLogType.ERROR);
                    deleteDirectoryRecursively(destPath);
                } catch (Exception ignore) {}
            });
            // 只复制manifest.json和对应平台的pages-xx.json
            Path srcPath = java.nio.file.Paths.get(srcDir);
            // manifest.json
            Path manifestSrc = srcPath.resolve("manifest.json");
            Path manifestDest = destPath.resolve("manifest.json");
            java.nio.file.Files.copy(manifestSrc, manifestDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // manifest.json回滚动作：写入前先备份，写入后立即添加回滚
            String manifestBackup = manifestDest.toString() + ".bak";
            java.nio.file.Files.copy(manifestDest, java.nio.file.Paths.get(manifestBackup), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：还原manifest.json",CreateNovelLogType.ERROR);
                    java.nio.file.Files.copy(java.nio.file.Paths.get(manifestBackup), manifestDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(manifestBackup));
                } catch (Exception ignore) {}
            });
            // pages-xx.json
            final String pagesFileName;
            switch (platform) {
                case "douyin":
                    pagesFileName = "pages-tt.json";
                    break;
                case "kuaishou":
                    pagesFileName = "pages-ks.json";
                    break;
                case "weixin":
                    pagesFileName = "pages-wx.json";
                    break;
                default:
                    throw new RuntimeException("不支持的平台类型: " + platform);
            }
            Path pagesSrc = srcPath.resolve(pagesFileName);
            Path pagesDest = destPath.resolve(pagesFileName);
            java.nio.file.Files.copy(pagesSrc, pagesDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // pages-xx.json回滚动作：写入前先备份，写入后立即添加回滚
            String pagesBackup = pagesDest.toString() + ".bak";
            java.nio.file.Files.copy(pagesDest, java.nio.file.Paths.get(pagesBackup), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：还原"+pagesFileName,CreateNovelLogType.ERROR);
                    java.nio.file.Files.copy(java.nio.file.Paths.get(pagesBackup), pagesDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(pagesBackup));
                } catch (Exception ignore) {}
            });
            // 2-1-2 编辑manifest.json内容
            if (withLogAndDelay) {
                taskLogger.log(taskId, "[2-1-2] 开始编辑manifest.json", CreateNovelLogType.PROCESSING);
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            try {
                taskLogger.log(taskId, "[2-1-2-1] 读取manifest.json", CreateNovelLogType.INFO);
                String manifestContent = new String(java.nio.file.Files.readAllBytes(manifestDest), java.nio.charset.StandardCharsets.UTF_8);
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.node.ObjectNode manifestNode = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(manifestContent);
                if (withLogAndDelay) {
                    try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                // 替换name字段
                taskLogger.log(taskId, "[2-1-2-2] 替换name字段", CreateNovelLogType.INFO);
                manifestNode.put("name", baseConfig.getAppName());
                if (withLogAndDelay) {
                    try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                // 替换平台appid
                taskLogger.log(taskId, "[2-1-2-3] 替换平台appid", CreateNovelLogType.INFO);
                switch (platform) {
                    case "douyin":
                        if (manifestNode.has("mp-toutiao")) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode)manifestNode.get("mp-toutiao")).put("appid", baseConfig.getAppid());
                        }
                        break;
                    case "kuaishou":
                        if (manifestNode.has("mp-kuaishou")) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode)manifestNode.get("mp-kuaishou")).put("appid", baseConfig.getAppid());
                        }
                        break;
                    case "weixin":
                        if (manifestNode.has("mp-weixin")) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode)manifestNode.get("mp-weixin")).put("appid", baseConfig.getAppid());
                        }
                        break;
                }
                if (withLogAndDelay) {
                    try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                // 写回文件
                taskLogger.log(taskId, "[2-1-2-4] 写回manifest.json", CreateNovelLogType.INFO);
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestDest.toFile(), manifestNode);
                taskLogger.log(taskId, "[2-1-2] manifest.json 编辑完成", CreateNovelLogType.SUCCESS);
                String formattedManifestContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifestNode);
                taskLogger.log(taskId,formattedManifestContent, CreateNovelLogType.INFO);
                // 操作成功后删除manifest.bak
                try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(manifestBackup)); } catch (Exception ignore) {}
            } catch (Exception e) {
                // 还原备份
                taskLogger.log(taskId, "回滚动作：还原manifest.json",CreateNovelLogType.ERROR);
                java.nio.file.Files.copy(java.nio.file.Paths.get(manifestBackup), manifestDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(manifestBackup));
                throw new RuntimeException("manifest.json内容编辑失败: " + e.getMessage(), e);
            }
            //2-1-3 编辑pages-xx.json文件
            if (withLogAndDelay) {
                taskLogger.log(taskId, "[2-1-3] 开始编辑" + pagesFileName, CreateNovelLogType.PROCESSING);
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            try {
                taskLogger.log(taskId, "[2-1-3-1] 读取" + pagesFileName, CreateNovelLogType.INFO);
                String pagesContent = new String(java.nio.file.Files.readAllBytes(pagesDest), java.nio.charset.StandardCharsets.UTF_8);
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(pagesContent);
                if (withLogAndDelay) {
                    try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                if (rootNode.has("pages") && rootNode.get("pages").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode pageNode : rootNode.get("pages")) {
                        if (!pageNode.has("path") || !pageNode.has("style")) continue;
                        String path = pageNode.get("path").asText();
                        com.fasterxml.jackson.databind.node.ObjectNode styleNode = (com.fasterxml.jackson.databind.node.ObjectNode) pageNode.get("style");
                        // navigationBarTitleText
                        if (path.equals("pages/homePage/homePage") || path.equals("pages/bookPage/bookPage") || path.equals("pages/filterPage/filterPage") || path.equals("pages/minePage/minePage")) {
                            styleNode.put("navigationBarTitleText", baseConfig.getAppName());
                        }
                        // navigationBarBackgroundColor
                        if (path.equals("pages/homePage/homePage") || path.equals("pages/bookPage/bookPage") || path.equals("pages/filterPage/filterPage") || path.equals("pages/detailPage/detailPage") || path.equals("pages/minePage/minePage")) {
                            String secondTheme = baseConfig.getSecondTheme();
                            if (secondTheme != null && secondTheme.matches("#?[A-Fa-f0-9]{8}")) {
                                // 转换#RRGGBBAA为#RRGGBB
                                if (secondTheme.startsWith("#")) {
                                    secondTheme = secondTheme.substring(0, 7);
                                } else {
                                    secondTheme = "#" + secondTheme.substring(0, 6);
                                }
                            }
                            styleNode.put("navigationBarBackgroundColor", secondTheme);
                        }
                    }
                }
                if (withLogAndDelay) {
                    try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                // 写回文件
                taskLogger.log(taskId, "[2-1-3-2] 写回" + pagesFileName, CreateNovelLogType.INFO);
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(pagesDest.toFile(), rootNode);
                taskLogger.log(taskId, "[2-1-3] " + pagesFileName + " 编辑完成", CreateNovelLogType.SUCCESS);
                String formattedPagesContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                taskLogger.log(taskId, formattedPagesContent, CreateNovelLogType.INFO);
                // 操作成功后删除pages.bak
                try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(pagesBackup)); } catch (Exception ignore) {}
            } catch (Exception e) {
                // 还原备份
                taskLogger.log(taskId, "回滚动作：还原"+pagesFileName,CreateNovelLogType.ERROR);
                java.nio.file.Files.copy(java.nio.file.Paths.get(pagesBackup), pagesDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(pagesBackup));
                throw new RuntimeException(pagesFileName+"内容编辑失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            taskLogger.log(taskId, "[2-1]目录prebuild/build处理失败: " + e.getMessage(), CreateNovelLogType.ERROR);
            // 回滚自身
            try {
                taskLogger.log(taskId, "回滚动作：删除" + destDir, CreateNovelLogType.ERROR);
                Path destPath = java.nio.file.Paths.get(destDir);
                deleteDirectoryRecursively(destPath);
            } catch (Exception ignore) {}
            throw new RuntimeException("目录prebuild/build处理失败: " + e.getMessage(), e);
        }
    }
    // 2. processThemeFile
    private void processThemeFile(String taskId, String buildCode, CreateNovelAppRequest.BaseConfig baseConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-2] 开始处理主题文件: " + buildWorkPath + File.separator + "src" + File.separator + "common" + File.separator + "styles" + File.separator + "theme.less", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String themeFilePath = buildWorkPath + File.separator + "src" + File.separator + "common" + File.separator + "styles" + File.separator + "theme.less";
        java.nio.file.Path themePath = java.nio.file.Paths.get(themeFilePath);
        String backupPath = themeFilePath + ".bak";
        try {
            // 备份原文件
            java.nio.file.Files.copy(themePath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // 立即添加回滚动作，确保后续任何失败都能回滚主题文件
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：还原主题文件",CreateNovelLogType.ERROR);
                    java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), themePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                } catch (Exception ignore) {}
            });
            taskLogger.log(taskId, "[2-2-1] 备份主题文件完成", CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 读取原内容
            java.util.List<String> lines = java.nio.file.Files.readAllLines(themePath, java.nio.charset.StandardCharsets.UTF_8);
            // 构造新主题色变量
            String mainTheme = baseConfig.getMainTheme();
            String secondTheme = baseConfig.getSecondTheme();
            String line1 = "@primary-color-" + buildCode + ": " + mainTheme + ";";
            String line2 = "@second-color-" + buildCode + ": " + secondTheme + ";";
            boolean foundPrimary = false, foundSecond = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("@primary-color-" + buildCode + ":")) {
                    lines.set(i, line1);
                    foundPrimary = true;
                }
                if (line.startsWith("@second-color-" + buildCode + ":")) {
                    lines.set(i, line2);
                    foundSecond = true;
                }
            }
            if (!foundPrimary) lines.add(line1);
            if (!foundSecond) lines.add(line2);
            // 写回文件
            java.nio.file.Files.write(themePath, lines, java.nio.charset.StandardCharsets.UTF_8);
            taskLogger.log(taskId, "[2-2-2] 新增主题色变量完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, "[2-2-3] 新增内容：\n" + line1 + "\n" + line2, CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除theme.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), themePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignore) {}
            throw new RuntimeException("主题文件处理失败: " + e.getMessage(), e);
        }
    }
    // 3. processDouyinPrefetchFile
    private void processDouyinPrefetchFile(String taskId, String buildCode, String platform, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-3] 开始处理抖音预取文件: " + buildWorkPath + File.separator + "prefetchbuild" + File.separator + "prelaunch-" + buildCode + ".js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        if (!"douyin".equals(platform)) return;
        String prefetchDir = buildWorkPath + File.separator + "prefetchbuild";
        String srcFile = prefetchDir + File.separator + "prelaunch-fun.js";
        String destFile = prefetchDir + File.separator + "prelaunch-" + buildCode + ".js";
        java.nio.file.Path srcPath = java.nio.file.Paths.get(srcFile);
        java.nio.file.Path destPath = java.nio.file.Paths.get(destFile);
        String backupPath = destFile + ".bak";
        try {
            // 复制原文件
            java.nio.file.Files.copy(srcPath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // 立即添加回滚动作
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：删除" + destFile, CreateNovelLogType.ERROR);
                    java.nio.file.Files.deleteIfExists(destPath);
                } catch (Exception ignore) {}
            });
            // 备份一份用于内容回滚
            java.nio.file.Files.copy(destPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：还原" + destFile, CreateNovelLogType.ERROR);
                    java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                } catch (Exception ignore) {}
            });
            taskLogger.log(taskId, "[2-3-1] 复制prelaunch-fun.js完成", CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 读取内容并修改brand
            String content = new String(java.nio.file.Files.readAllBytes(destPath), java.nio.charset.StandardCharsets.UTF_8);
            // 简单正则替换 param.brand = 'xxx' 或 param.brand: 'xxx'
            String newContent = content.replaceAll("(brand\\s*:\\s*)['\"][^'\"]+['\"]", "$1'" + buildCode + "'");
            java.nio.file.Files.write(destPath, newContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-3-2] 修改param.brand为" + buildCode + "完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, newContent, CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除prelaunch.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.deleteIfExists(destPath); } catch (Exception ignore) {}
            throw new RuntimeException("抖音预取文件处理失败: " + e.getMessage(), e);
        }
    }
    // 4. processBaseConfigFile
    private void processBaseConfigFile(String taskId, String buildCode, String platform, CreateNovelAppRequest.BaseConfig baseConfig, CreateNovelAppRequest.CommonConfig commonConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-4-1] 开始处理baseConfig配置文件: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "baseConfigs" + File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "baseConfigs";
        String configFile = configDir + File.separator + buildCode + ".js";
        java.nio.file.Path configPath = java.nio.file.Paths.get(configFile);
        String backupPath = configFile + ".bak";
        try {
            // 确保目录存在
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(configDir));
            // 如果文件已存在，先备份
            if (java.nio.file.Files.exists(configPath)) {
                java.nio.file.Files.copy(configPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原baseConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                // 文件不存在，回滚时删除
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除baseConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.deleteIfExists(configPath);
                    } catch (Exception ignore) {}
                });
            }
            // 构造内容
            StringBuilder sb = new StringBuilder();
            sb.append("export default {\n");
            sb.append("  AppConfig: {\n");

            // tt
            sb.append("    'tt': {");
            if ("douyin".equals(platform)) {
                sb.append("\n");
                sb.append("      \"app_name\": \"").append(baseConfig.getAppName()).append("\",\n");
                sb.append("      \"app_code\": \"").append(baseConfig.getAppCode()).append("\",\n");
                sb.append("      \"code\": \"").append(baseConfig.getPlatform()).append("\",\n");
                sb.append("      \"product\": \"").append(baseConfig.getProduct()).append("\",\n");
                sb.append("      \"customer\": \"").append(baseConfig.getCustomer()).append("\",\n");
                sb.append("      \"appid\": \"").append(baseConfig.getAppid()).append("\",\n");
                sb.append("      \"token_id\": ").append(baseConfig.getTokenId()).append(",\n");
                sb.append("      \"version\": \"").append(baseConfig.getVersion()).append("\",\n");
                sb.append("      \"cl\": \"").append(baseConfig.getCl()).append("\"\n");
            }
            sb.append("    },\n");

            // ks
            sb.append("    'ks': {");
            if ("kuaishou".equals(platform)) {
                sb.append("\n");
                sb.append("      \"app_name\": \"").append(baseConfig.getAppName()).append("\",\n");
                sb.append("      \"app_code\": \"").append(baseConfig.getAppCode()).append("\",\n");
                sb.append("      \"code\": \"").append(baseConfig.getPlatform()).append("\",\n");
                sb.append("      \"client_id\": \"").append(commonConfig.getKuaishouClientId()).append("\",\n");
                sb.append("      \"client_secret\": \"").append(commonConfig.getKuaishouClientSecret()).append("\",\n");
                sb.append("      \"product\": \"").append(baseConfig.getProduct()).append("\",\n");
                sb.append("      \"customer\": \"").append(baseConfig.getCustomer()).append("\",\n");
                sb.append("      \"appid\": \"").append(baseConfig.getAppid()).append("\",\n");
                sb.append("      \"token_id\": ").append(baseConfig.getTokenId()).append(",\n");
                sb.append("      \"version\": \"").append(baseConfig.getVersion()).append("\",\n");
                sb.append("      \"cl\": \"").append(baseConfig.getCl()).append("\"\n");
            }
            sb.append("    },\n");

            // wx
            sb.append("    'wx': {");
            if ("weixin".equals(platform)) {
                sb.append("\n");
                sb.append("      \"app_name\": \"").append(baseConfig.getAppName()).append("\",\n");
                sb.append("      \"app_code\": \"").append(baseConfig.getAppCode()).append("\",\n");
                sb.append("      \"product\": \"").append(baseConfig.getProduct()).append("\",\n");
                sb.append("      \"customer\": \"").append(baseConfig.getCustomer()).append("\",\n");
                sb.append("      \"appid\": \"").append(baseConfig.getAppid()).append("\",\n");
                sb.append("      \"token_id\": ").append(baseConfig.getTokenId()).append(",\n");
                sb.append("      \"version\": \"").append(baseConfig.getVersion()).append("\",\n");
                sb.append("      \"cl\": \"").append(baseConfig.getCl()).append("\"\n");
            }
            sb.append("    },\n");

            // bd
            sb.append("    'bd': {}\n");

            sb.append("  }\n");
            sb.append("}\n");
            String fileContent = sb.toString();
            java.nio.file.Files.write(configPath, fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-4-1] baseConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, fileContent, CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.deleteIfExists(configPath); } catch (Exception ignore) {}
            throw new RuntimeException("baseConfig配置文件处理失败: " + e.getMessage(), e);
        }
    }
    // 5. processAdConfigFile
    private void processAdConfigFile(String taskId, String buildCode, String platform, CreateNovelAppRequest.AdConfig adConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-4-3] 开始处理adConfig配置文件: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "adConfigs" + File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "adConfigs";
        String configFile = configDir + File.separator + buildCode + ".js";
        java.nio.file.Path configPath = java.nio.file.Paths.get(configFile);
        String backupPath = configFile + ".bak";
        try {
            // 确保目录存在
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(configDir));
            // 如果文件已存在，先备份
            if (java.nio.file.Files.exists(configPath)) {
                java.nio.file.Files.copy(configPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原adConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                // 文件不存在，回滚时删除
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除adConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.deleteIfExists(configPath);
                    } catch (Exception ignore) {}
                });
            }
            // 构造内容
            StringBuilder sb = new StringBuilder();
            sb.append("export default {\n");
            // 平台列表
            String[] platforms = {"tt", "ks", "wx", "bd"};
            for (String pf : platforms) {
                sb.append("    '").append(pf).append("': {\n");
                // 默认结构
                sb.append("        rewardAd: {\n            enable: false\n        },\n");
                sb.append("        native: {\n            enable: false\n        },\n");
                sb.append("        interstitial: {\n            enable: false\n        }\n");
                sb.append("    },\n");
            }
            // 移除最后一个逗号
            int lastComma = sb.lastIndexOf(",\n}");
            if (lastComma != -1) {
                sb.delete(lastComma, lastComma + 2);
            }
            sb.append("}\n");
            // 解析为对象再写入广告配置
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // 先构造一个Map结构
            java.util.LinkedHashMap<String, Object> adConfigMap = new java.util.LinkedHashMap<>();
            for (String pf : platforms) {
                java.util.LinkedHashMap<String, Object> pfMap = new java.util.LinkedHashMap<>();
                pfMap.put("rewardAd", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                pfMap.put("native", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                pfMap.put("interstitial", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                adConfigMap.put(pf, pfMap);
            }
            // 写入对应平台的广告配置
            if (adConfig != null) {
                // rewardAd
                if (adConfig.getRewardAd() != null) {
                    java.util.Map<String, Object> rewardAd = new java.util.LinkedHashMap<>();
                    boolean enable = Boolean.TRUE.equals(adConfig.getRewardAd().getEnabled());
                    rewardAd.put("enable", enable);
                    if (enable) {
                        rewardAd.put("id", adConfig.getRewardAd().getRewardAdId());
                        rewardAd.put("count", adConfig.getRewardAd().getRewardCount());
                    }
                    ((java.util.Map<String, Object>)adConfigMap.get(platformToKey(platform))).put("rewardAd", rewardAd);
                }
                // interstitialAd
                if (adConfig.getInterstitialAd() != null) {
                    java.util.Map<String, Object> interstitialAd = new java.util.LinkedHashMap<>();
                    boolean enable = Boolean.TRUE.equals(adConfig.getInterstitialAd().getEnabled());
                    interstitialAd.put("enable", enable);
                    if (enable) {
                        Object idVal = adConfig.getInterstitialAd().getInterstitialAdId();
                        if ("kuaishou".equals(platform) && idVal != null) {
                            try {
                                interstitialAd.put("id", Long.parseLong(idVal.toString()));
                            } catch (Exception ignore) { interstitialAd.put("id", idVal); }
                        } else {
                            interstitialAd.put("id", idVal);
                        }
                        interstitialAd.put("count", adConfig.getInterstitialAd().getInterstitialCount());
                    }
                    ((java.util.Map<String, Object>)adConfigMap.get(platformToKey(platform))).put("interstitial", interstitialAd);
                }
                // nativeAd
                if (adConfig.getNativeAd() != null) {
                    java.util.Map<String, Object> nativeAd = new java.util.LinkedHashMap<>();
                    boolean enable = Boolean.TRUE.equals(adConfig.getNativeAd().getEnabled());
                    nativeAd.put("enable", enable);
                    if (enable) {
                        nativeAd.put("id", adConfig.getNativeAd().getNativeAdId());
                    }
                    ((java.util.Map<String, Object>)adConfigMap.get(platformToKey(platform))).put("native", nativeAd);
                }
            }
            // 生成最终内容
            StringBuilder finalSb = new StringBuilder();
            finalSb.append("export default ");
            finalSb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adConfigMap));
            finalSb.append(";\n");
            java.nio.file.Files.write(configPath, finalSb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-4-3] adConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, finalSb.toString(), CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.deleteIfExists(configPath); } catch (Exception ignore) {}
            throw new RuntimeException("adConfig配置文件处理失败: " + e.getMessage(), e);
        }
    }
    // 6. processPayConfigFile
    private void processPayConfigFile(String taskId, String buildCode, String platform, CreateNovelAppRequest.PaymentConfig payConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-4-4] 开始处理payConfig配置文件: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "payConfigs" + File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "payConfigs";
        String configFile = configDir + File.separator + buildCode + ".js";
        java.nio.file.Path configPath = java.nio.file.Paths.get(configFile);
        String backupPath = configFile + ".bak";
        try {
            // 确保目录存在
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(configDir));
            // 如果文件已存在，先备份
            if (java.nio.file.Files.exists(configPath)) {
                java.nio.file.Files.copy(configPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原payConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                // 文件不存在，回滚时删除
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除payConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.deleteIfExists(configPath);
                    } catch (Exception ignore) {}
                });
            }
            // 构造内容
            java.util.LinkedHashMap<String, Object> payConfigMap = new java.util.LinkedHashMap<>();
            // 默认结构
            payConfigMap.put("tt", new java.util.LinkedHashMap<String, Object>() {{
                put("normal_pay", buildPayTypeMap(null));
                put("order_pay", buildPayTypeMap(null));
                put("renew_pay", buildPayTypeMap(null));
                put("dou_zuan_pay", buildPayTypeMap(null));
            }});
            payConfigMap.put("ks", new java.util.LinkedHashMap<String, Object>() {{
                put("normal_pay", buildPayTypeMap(null));
                put("order_pay", buildPayTypeMap(null));
                put("renew_pay", buildPayTypeMap(null));
            }});
            payConfigMap.put("wx", new java.util.LinkedHashMap<String, Object>() {{
                put("normal_pay", buildPayTypeMap(null));
                put("order_pay", buildPayTypeMap(null));
                put("renew_pay", buildPayTypeMap(null));
                put("wx_virtual_pay", buildPayTypeMap(null));
            }});
            payConfigMap.put("bd", new java.util.LinkedHashMap<String, Object>() {{
                put("normal_pay", buildPayTypeMap(null));
                put("order_pay", buildPayTypeMap(null));
            }});
            // 只写入当前平台
            String key = platformToKey(platform);
            switch (key) {
                case "tt":
                    ((java.util.Map<String, Object>)payConfigMap.get("tt")).put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("tt")).put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("tt")).put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("tt")).put("dou_zuan_pay", buildPayTypeMap(payConfig.getDouzuanPay()));
                    break;
                case "ks":
                    ((java.util.Map<String, Object>)payConfigMap.get("ks")).put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("ks")).put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("ks")).put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                    break;
                case "wx":
                    ((java.util.Map<String, Object>)payConfigMap.get("wx")).put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("wx")).put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("wx")).put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("wx")).put("wx_virtual_pay", buildPayTypeMap(payConfig.getWxVirtualPay()));
                    break;
                case "bd":
                    ((java.util.Map<String, Object>)payConfigMap.get("bd")).put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                    ((java.util.Map<String, Object>)payConfigMap.get("bd")).put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                    break;
            }
            // 生成最终内容
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            StringBuilder finalSb = new StringBuilder();
            finalSb.append("export default ");
            finalSb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payConfigMap));
            finalSb.append(";\n");
            java.nio.file.Files.write(configPath, finalSb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-4-4] payConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, finalSb.toString(), CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.deleteIfExists(configPath); } catch (Exception ignore) {}
            throw new RuntimeException("payConfig配置文件处理失败: " + e.getMessage(), e);
        }
    }
    // 7. processDeliverConfigFile
    private void processDeliverConfigFile(String taskId, String buildCode, String platform, CreateNovelAppRequest.DeliverConfig deliverConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-4-5] 开始处理deliverConfig配置文件: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "deliverConfigs" + File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "deliverConfigs";
        String configFile = configDir + File.separator + buildCode + ".js";
        java.nio.file.Path configPath = java.nio.file.Paths.get(configFile);
        String backupPath = configFile + ".bak";
        try {
            // 确保目录存在
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(configDir));
            // 如果文件已存在，先备份
            if (java.nio.file.Files.exists(configPath)) {
                java.nio.file.Files.copy(configPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原deliverConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                // 文件不存在，回滚时删除
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除deliverConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.deleteIfExists(configPath);
                    } catch (Exception ignore) {}
                });
            }
            // 构造内容
            java.util.LinkedHashMap<String, Object> deliverConfigMap = new java.util.LinkedHashMap<>();
            String[] platforms = {"tt", "ks", "wx", "bd"};
            for (String pf : platforms) {
                java.util.LinkedHashMap<String, Object> pfMap = new java.util.LinkedHashMap<>();
                pfMap.put("deliver_id", "");
                pfMap.put("banner_id", "");
                pfMap.put("deliverAdId", "");
                pfMap.put("bannerAdId", "");
                pfMap.put("enable", true);
                pfMap.put("useTest", false);
                deliverConfigMap.put(pf, pfMap);
            }
            // 根据platform写入对应数据
            String key = platformToKey(platform);
            if (deliverConfig != null && deliverConfigMap.containsKey(key)) {
                java.util.Map<String, Object> pfMap = (java.util.Map<String, Object>) deliverConfigMap.get(key);
                pfMap.put("deliver_id", deliverConfig.getDeliverId() != null ? deliverConfig.getDeliverId() : "");
                pfMap.put("banner_id", deliverConfig.getBannerId() != null ? deliverConfig.getBannerId() : "");
            }
            // 生成最终内容
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            StringBuilder finalSb = new StringBuilder();
            finalSb.append("export default ");
            finalSb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deliverConfigMap));
            finalSb.append(";\n");
            java.nio.file.Files.write(configPath, finalSb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-4-5] deliverConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, finalSb.toString(), CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.deleteIfExists(configPath); } catch (Exception ignore) {}
            throw new RuntimeException("deliverConfig配置文件处理失败: " + e.getMessage(), e);
        }
    }
    // 8. processCommonConfigFile
    private void processCommonConfigFile(String taskId, String buildCode, String platform, CreateNovelAppRequest.CommonConfig commonConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-4-6] 开始处理commonConfig配置文件: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "commonConfigs" + File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "commonConfigs";
        String configFile = configDir + File.separator + buildCode + ".js";
        java.nio.file.Path configPath = java.nio.file.Paths.get(configFile);
        String backupPath = configFile + ".bak";
        try {
            // 确保目录存在
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(configDir));
            // 如果文件已存在，先备份
            if (java.nio.file.Files.exists(configPath)) {
                java.nio.file.Files.copy(configPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原commonConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                // 文件不存在，回滚时删除
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除commonConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.deleteIfExists(configPath);
                    } catch (Exception ignore) {}
                });
            }
            // 构造内容
            java.util.LinkedHashMap<String, Object> commonConfigMap = new java.util.LinkedHashMap<>();
            String[] platforms = {"tt", "ks", "wx", "bd"};
            for (String pf : platforms) {
                java.util.LinkedHashMap<String, Object> pfMap = new java.util.LinkedHashMap<>();
                java.util.LinkedHashMap<String, Object> homeCard = new java.util.LinkedHashMap<>();
                homeCard.put("style", 1);
                java.util.LinkedHashMap<String, Object> payCard = new java.util.LinkedHashMap<>();
                payCard.put("style", 1);
                java.util.LinkedHashMap<String, Object> loginType = new java.util.LinkedHashMap<>();
                loginType.put("mine", "anonymousLogin");
                loginType.put("reader", "anonymousLogin");
                pfMap.put("homeCard", homeCard);
                pfMap.put("payCard", payCard);
                pfMap.put("loginType", loginType);
                pfMap.put("contact", "");
                // iaaMode 默认
                java.util.LinkedHashMap<String, Object> iaaModeObj = new java.util.LinkedHashMap<>();
                iaaModeObj.put("enable", false);
                iaaModeObj.put("dialogStyle", 2);
                pfMap.put("iaaMode", iaaModeObj);
                pfMap.put("hidePayEntry", false);

                if ("tt".equals(pf)) pfMap.put("imId", "");
                commonConfigMap.put(pf, pfMap);
            }
            // 只对当前platform赋值
            String key = platformToKey(platform);
            if (commonConfig != null && commonConfigMap.containsKey(key)) {
                java.util.Map<String, Object> pfMap = (java.util.Map<String, Object>) commonConfigMap.get(key);
                // homeCard.style
                java.util.Map<String, Object> homeCard = (java.util.Map<String, Object>) pfMap.get("homeCard");
                homeCard.put("style", commonConfig.getHomeCardStyle() != null ? commonConfig.getHomeCardStyle() : 1);
                // payCard.style
                java.util.Map<String, Object> payCard = (java.util.Map<String, Object>) pfMap.get("payCard");
                payCard.put("style", commonConfig.getPayCardStyle() != null ? commonConfig.getPayCardStyle() : 1);
                // loginType
                java.util.Map<String, Object> loginType = (java.util.Map<String, Object>) pfMap.get("loginType");
                loginType.put("mine", commonConfig.getMineLoginType() != null ? commonConfig.getMineLoginType() : "anonymousLogin");
                loginType.put("reader", commonConfig.getReaderLoginType() != null ? commonConfig.getReaderLoginType() : "anonymousLogin");
                // contact
                pfMap.put("contact", commonConfig.getContact() != null ? commonConfig.getContact() : "");
                // iaaMode
                java.util.LinkedHashMap<String, Object> iaaModeObj = new java.util.LinkedHashMap<>();
                iaaModeObj.put("enable", commonConfig.getIaaMode() != null ? commonConfig.getIaaMode() : false);
                iaaModeObj.put("dialogStyle", commonConfig.getIaaDialogStyle() != null ? commonConfig.getIaaDialogStyle() : 2);
                pfMap.put("iaaMode", iaaModeObj);
                pfMap.put("hidePayEntry", commonConfig.getHidePayEntry() != null ? commonConfig.getHidePayEntry() : false);
                // imId 仅tt
                if ("tt".equals(key)) pfMap.put("imId", commonConfig.getDouyinImId() != null ? commonConfig.getDouyinImId() : "");
            }
            // 生成最终内容
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            StringBuilder finalSb = new StringBuilder();
            finalSb.append("export default ");
            finalSb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(commonConfigMap));
            finalSb.append(";\n");
            java.nio.file.Files.write(configPath, finalSb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-4-6] commonConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, finalSb.toString(), CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.deleteIfExists(configPath); } catch (Exception ignore) {}
            throw new RuntimeException("commonConfig配置文件处理失败: " + e.getMessage(), e);
        }
    }
    // 9. processAppConfigFile
    private void processAppConfigFile(String taskId, String buildCode, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-5] 开始处理AppConfig.js: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "AppConfig.js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config";
        String configFile = configDir + File.separator + "AppConfig.js";
        java.nio.file.Path configPath = java.nio.file.Paths.get(configFile);
        String backupPath = configFile + ".bak";
        try {
            // 备份原文件
            if (java.nio.file.Files.exists(configPath)) {
                java.nio.file.Files.copy(configPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原AppConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                // 文件不存在，回滚时删除
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除AppConfig.js",CreateNovelLogType.ERROR);
                        java.nio.file.Files.deleteIfExists(configPath);
                    } catch (Exception ignore) {}
                });
            }
            // 读取原内容
            String content = java.nio.file.Files.exists(configPath) ? new String(java.nio.file.Files.readAllBytes(configPath), java.nio.charset.StandardCharsets.UTF_8) : "";
            java.util.List<String> lines = new java.util.ArrayList<>();
            if (!content.isEmpty()) {
                for (String line : content.split("\\r?\\n")) lines.add(line);
            }
            // 生成导入变量名和导入语句
            String baseVar = buildCode + "Config";
            String payVar = buildCode + "PayConfig";
            String adVar = buildCode + "AdConfig";
            String deliverVar = buildCode + "DeliverConfig";
            String commonVar = buildCode + "CommonConfig";
            String importBaseConfig = "import " + baseVar + " from './baseConfigs/" + buildCode + "';";
            String importPayConfig = "import " + payVar + " from './payConfigs/" + buildCode + "';";
            String importAdConfig = "import " + adVar + " from './adConfigs/" + buildCode + "';";
            String importDeliverConfig = "import " + deliverVar + " from './deliverConfigs/" + buildCode + "';";
            String importCommonConfig = "import " + commonVar + " from './commonConfigs/" + buildCode + "';";
            // 定义类型与导入语句映射
            java.util.LinkedHashMap<String, String> importMap = new java.util.LinkedHashMap<>();
            importMap.put("baseConfig", importBaseConfig);
            importMap.put("payConfig", importPayConfig);
            importMap.put("adConfig", importAdConfig);
            importMap.put("deliverConfig", importDeliverConfig);
            importMap.put("commonConfig", importCommonConfig);
            // 记录每种类型最后一行的下标
            java.util.Map<String, Integer> lastImportIndex = new java.util.HashMap<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                for (String key : importMap.keySet()) {
                    if (line.startsWith("import ") && line.contains(key)) {
                        lastImportIndex.put(key, i);
                    }
                }
            }
            // 检查是否已存在同样的导入
            java.util.Set<String> existingImports = new java.util.HashSet<>();
            for (String line : lines) {
                for (String imp : importMap.values()) {
                    if (line.trim().equals(imp)) {
                        existingImports.add(imp);
                    }
                }
            }
            // 插入导入语句
            for (String key : importMap.keySet()) {
                String imp = importMap.get(key);
                if (existingImports.contains(imp)) continue; // 已有则跳过
                Integer idx = lastImportIndex.get(key);
                if (idx != null) {
                    lines.add(idx + 1, imp);
                    // 更新所有后续下标
                    for (String k : lastImportIndex.keySet()) {
                        if (lastImportIndex.get(k) > idx) lastImportIndex.put(k, lastImportIndex.get(k) + 1);
                    }
                } else {
                    // 没有同类型导入，则插入到所有import语句最后一行后
                    int lastImport = -1;
                    for (int i = 0; i < lines.size(); i++) {
                        if (lines.get(i).trim().startsWith("import ")) lastImport = i;
                    }
                    lines.add(lastImport + 1, imp);
                }
            }

            // ========== 新增分支插入逻辑 ==========
            String upperBuildCode = buildCode.toUpperCase();
            // 1. getBrand 方法插入 #ifdef
            String getBrandIfdef = "    // #ifdef MP-" + upperBuildCode + "\n    return '" + buildCode + "'\n    // #endif ";
            // 找到 getBrand 方法体范围（兼容多行声明）
            int startIdx = -1, endIdx = -1, braceCount = 0;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.matches(".*(const|let)\\s+getBrand\\s*=.*=>.*") || line.contains("function getBrand")) {
                    // 向下找第一个 {
                    int j = i;
                    while (j < lines.size() && !lines.get(j).contains("{")) j++;
                    if (j == lines.size()) break;
                    startIdx = j;
                    braceCount = 1;
                    int k = j;
                    for (k = j + 1; k < lines.size(); k++) {
                        if (lines.get(k).contains("{")) braceCount++;
                        if (lines.get(k).contains("}")) braceCount--;
                        if (braceCount == 0) { endIdx = k; break; }
                    }
                    break;
                }
            }
            if (startIdx != -1 && endIdx != -1) {
                // 检查是否已存在该分支
                boolean already = false;
                for (int k = startIdx; k < endIdx; k++) {
                    if (lines.get(k).contains("#ifdef MP-" + upperBuildCode)) {
                        already = true; break;
                    }
                }
                if (!already) {
                    lines.add(endIdx, getBrandIfdef);
                }
            }
            // 2. loadPayConfig
            insertSwitchCaseCompat(lines, "loadPayConfig", buildCode, payVar);
            // 3. loadAdConfig
            insertSwitchCaseCompat(lines, "loadAdConfig", buildCode, adVar);
            // 4. loadDeliverConfig
            insertSwitchCaseCompat(lines, "loadDeliverConfig", buildCode, deliverVar);
            // 5. loadCommonConfig
            insertSwitchCaseCompat(lines, "loadCommonConfig", buildCode, commonVar);
            // 6. loadBrandConfig
            insertSwitchCaseCompat(lines, "loadBrandConfig", buildCode, baseVar);

            // 写回文件
            StringBuilder sb = new StringBuilder();
            for (String line : lines) sb.append(line).append("\n");
            java.nio.file.Files.write(configPath, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-5] AppConfig.js写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, sb.toString(), CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.deleteIfExists(configPath); } catch (Exception ignore) {}
            throw new RuntimeException("AppConfig.js处理失败: " + e.getMessage(), e);
        }
    }
    // 10. processPackageJsonFile
    private void processPackageJsonFile(String taskId, String buildCode, String platform, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-6] 开始处理package.json: " + buildWorkPath + File.separator + "package.json", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String packageJsonPath = buildWorkPath + File.separator + "package.json";
        java.nio.file.Path jsonPath = java.nio.file.Paths.get(packageJsonPath);
        String backupPath = packageJsonPath + ".bak";
        try {
            // 备份原文件
            if (java.nio.file.Files.exists(jsonPath)) {
                java.nio.file.Files.copy(jsonPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原package.json",CreateNovelLogType.ERROR);
                        java.nio.file.Files.copy(jsonPath, jsonPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除package.json",CreateNovelLogType.ERROR);
                        java.nio.file.Files.deleteIfExists(jsonPath);
                    } catch (Exception ignore) {}
                });
            }
            // 读取并解析JSON
            String content = java.nio.file.Files.exists(jsonPath) ? new String(java.nio.file.Files.readAllBytes(jsonPath), java.nio.charset.StandardCharsets.UTF_8) : "{}";
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(content);
            // 1. scripts 只处理当前平台
            com.fasterxml.jackson.databind.node.ObjectNode scripts = root.has("scripts") && root.get("scripts").isObject()
                    ? (com.fasterxml.jackson.databind.node.ObjectNode) root.get("scripts")
                    : objectMapper.createObjectNode();
            String upperBuildCode = buildCode.toUpperCase();
            String platKey = platformToKey(platform);
            String devKey = "dev:" + platKey + "-" + buildCode;
            String buildKey = "build:" + platKey + "-" + buildCode;
            String devCmd = "uni -p " + platKey + "-" + buildCode + " --minify";
            String buildCmd = "uni build -p " + platKey + "-" + buildCode + " --minify";
            scripts.put(devKey, devCmd);
            scripts.put(buildKey, buildCmd);
            root.set("scripts", scripts);
            // 2. uni-app.scripts 写入到 uni-app 节点下的 scripts 子节点
            com.fasterxml.jackson.databind.node.ObjectNode uniApp = root.has("uni-app") && root.get("uni-app").isObject()
                    ? (com.fasterxml.jackson.databind.node.ObjectNode) root.get("uni-app")
                    : objectMapper.createObjectNode();
            com.fasterxml.jackson.databind.node.ObjectNode uniAppScripts = uniApp.has("scripts") && uniApp.get("scripts").isObject()
                    ? (com.fasterxml.jackson.databind.node.ObjectNode) uniApp.get("scripts")
                    : objectMapper.createObjectNode();
            String key = platKey + "-" + buildCode;
            com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();
            com.fasterxml.jackson.databind.node.ObjectNode env = objectMapper.createObjectNode();
            com.fasterxml.jackson.databind.node.ObjectNode define = objectMapper.createObjectNode();
            switch (platKey) {
                case "tt": env.put("UNI_PLATFORM", "mp-toutiao"); break;
                case "ks": env.put("UNI_PLATFORM", "mp-kuaishou"); break;
                case "wx": env.put("UNI_PLATFORM", "mp-weixin"); break;
                case "bd": env.put("UNI_PLATFORM", "mp-baidu"); break;
            }
            define.put("MP-" + upperBuildCode, true);
            node.set("env", env);
            node.set("define", define);
            uniAppScripts.set(key, node);
            uniApp.set("scripts", uniAppScripts);
            root.set("uni-app", uniApp);
            // 写回文件
            String finalContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            java.nio.file.Files.write(jsonPath, finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-6] package.json写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, finalContent, CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { java.nio.file.Files.deleteIfExists(jsonPath); } catch (Exception ignore) {}
            throw new RuntimeException("package.json处理失败: " + e.getMessage(), e);
        }
    }
    // 11. insertSwitchCaseCompat
    private void insertSwitchCaseCompat(java.util.List<String> lines, String funcName, String buildCode, String varName) {
        String caseStr = "        case '" + buildCode + "':\n            return " + varName + ";";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("function " + funcName) || line.matches(".*(const|let)\\s+" + funcName + "\\s*=.*=>.*\\{")) {
                // 找到switch
                int switchIdx = -1;
                for (int j = i; j < lines.size(); j++) {
                    if (lines.get(j).contains("switch")) {
                        switchIdx = j;
                        break;
                    }
                }
                if (switchIdx != -1) {
                    // 检查是否已存在该case
                    boolean already = false;
                    int insertIdx = switchIdx + 1;
                    while (insertIdx < lines.size() && !lines.get(insertIdx).contains("}")) {
                        if (lines.get(insertIdx).contains("case '" + buildCode + "':")) {
                            already = true;
                            break;
                        }
                        insertIdx++;
                    }
                    if (!already) {
                        lines.add(insertIdx, caseStr);
                    }
                }
                break;
            }
        }
    }
    // 12. buildPayTypeMap
    private java.util.Map<String, Object> buildPayTypeMap(CreateNovelAppRequest.PayTypeConfig payTypeConfig) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (payTypeConfig == null || !Boolean.TRUE.equals(payTypeConfig.getEnabled())) {
            map.put("enable", false);
        } else {
            map.put("enable", true);
            java.util.Map<String, Object> gatewayId = new java.util.LinkedHashMap<>();
            try {
                gatewayId.put("android", payTypeConfig.getGatewayAndroid() == null ? 0 : Integer.parseInt(payTypeConfig.getGatewayAndroid()));
            } catch (Exception e) { gatewayId.put("android", payTypeConfig.getGatewayAndroid()); }
            try {
                gatewayId.put("ios", payTypeConfig.getGatewayIos() == null ? 0 : Integer.parseInt(payTypeConfig.getGatewayIos()));
            } catch (Exception e) { gatewayId.put("ios", payTypeConfig.getGatewayIos()); }
            map.put("gateway_id", gatewayId);
        }
        return map;
    }
    // 13. platformToKey
    private String platformToKey(String platform) {
        switch (platform) {
            case "douyin": return "tt";
            case "kuaishou": return "ks";
            case "weixin": return "wx";
            case "baidu": return "bd";
            default: return platform;
        }
    }
    // 14. deleteDirectoryRecursively
    private void deleteDirectoryRecursively(Path path) throws java.io.IOException {
        if (java.nio.file.Files.notExists(path)) return;
        java.nio.file.Files.walk(path)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try { java.nio.file.Files.deleteIfExists(p); } catch (java.io.IOException ignore) {}
                });
    }
} 
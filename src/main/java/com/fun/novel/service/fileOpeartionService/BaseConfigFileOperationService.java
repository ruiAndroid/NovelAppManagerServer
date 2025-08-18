package com.fun.novel.service.fileOpeartionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppLocalFileOperationService;
import com.fun.novel.dto.CreateNovelLogType;
import org.springframework.stereotype.Service;
// Removed @Service annotation to avoid multiple beans of NovelAppLocalFileOperationService type

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 基础配置文件操作服务
 * 处理manifest.json、pages-xx.json、theme.less等基础配置文件
 * 
 * Note: This class is not annotated with @Service to avoid being registered as a Spring Bean.
 * It is intended to be used as a base class for other services.
 */
@Service
public class BaseConfigFileOperationService extends AbstractConfigFileOperationService{




    public void createBaseConfigLocalCodeFiles(String taskId,CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        createPrebuildBuildDir(taskId, buildCode, platform, baseConfig, rollbackActions, true);
        createThemeFile(taskId, buildCode, baseConfig, rollbackActions, true);
        createDouyinPrefetchFile(taskId, buildCode, platform, rollbackActions, true);
        createBaseConfigFile(taskId, buildCode, platform, baseConfig, commonConfig, rollbackActions, true);
        createDeliverConfigFile(taskId, buildCode, platform, params.getDeliverConfig(),rollbackActions, true);

    }

    public void updateBaseConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();


        createThemeFile(null, buildCode, baseConfig, rollbackActions, false);
        createBaseConfigFile(null, buildCode, platform, baseConfig, commonConfig, rollbackActions, false);
        createDeliverConfigFile(null, buildCode, platform, params.getDeliverConfig(),rollbackActions, false);
    }

    // 以下是处理各种基础配置文件的具体实现方法
    
    /**
     * 处理prebuild/build目录
     */
    private void createPrebuildBuildDir(String taskId, String buildCode, String platform,
                                       CreateNovelAppRequest.BaseConfig baseConfig, 
                                       List<Runnable> rollbackActions, boolean withLogAndDelay) {
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
    
    /**
     * 处理主题文件
     */
    private void createThemeFile(String taskId, String buildCode, CreateNovelAppRequest.BaseConfig baseConfig,
                                List<Runnable> rollbackActions, boolean withLogAndDelay) {
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
                    foundPrimary = true;
                }
                if (line.startsWith("@second-color-" + buildCode + ":")) {
                    foundSecond = true;
                }
            }

            // 如果已经存在主题色配置，则忽略不处理
            if (foundPrimary && foundSecond) {
                taskLogger.log(taskId, "[2-2-2] 主题色变量已存在，跳过处理", CreateNovelLogType.SUCCESS);
                // 操作成功后删除theme.bak
                try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
                return;
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
    
    /**
     * 处理抖音预取文件
     */
    private void createDouyinPrefetchFile(String taskId, String buildCode, String platform,
                                         List<Runnable> rollbackActions, boolean withLogAndDelay) {
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
    
    /**
     * 处理基础配置文件
     */
    private void createBaseConfigFile(String taskId, String buildCode, String platform,
                                     CreateNovelAppRequest.BaseConfig baseConfig, 
                                     CreateNovelAppRequest.CommonConfig commonConfig, 
                                     List<Runnable> rollbackActions, boolean withLogAndDelay) {
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
            
            // 构造当前平台的配置内容
            StringBuilder currentPlatformConfig = new StringBuilder();
            if ("douyin".equals(platform)) {
                currentPlatformConfig.append("      \"app_name\": \"").append(baseConfig.getAppName()).append("\",\n");
                currentPlatformConfig.append("      \"app_code\": \"").append(baseConfig.getAppCode()).append("\",\n");
                currentPlatformConfig.append("      \"code\": \"").append("toutiao").append("\",\n");
                currentPlatformConfig.append("      \"product\": \"").append(baseConfig.getProduct()).append("\",\n");
                currentPlatformConfig.append("      \"customer\": \"").append(baseConfig.getCustomer()).append("\",\n");
                currentPlatformConfig.append("      \"appid\": \"").append(baseConfig.getAppid()).append("\",\n");
                currentPlatformConfig.append("      \"token_id\": ").append(baseConfig.getTokenId()).append(",\n");
                currentPlatformConfig.append("      \"version\": \"").append(baseConfig.getVersion()).append("\",\n");
                currentPlatformConfig.append("      \"cl\": \"").append(baseConfig.getCl()).append("\"\n");
            } else if ("kuaishou".equals(platform)) {
                currentPlatformConfig.append("      \"app_name\": \"").append(baseConfig.getAppName()).append("\",\n");
                currentPlatformConfig.append("      \"app_code\": \"").append(baseConfig.getAppCode()).append("\",\n");
                currentPlatformConfig.append("      \"code\": \"").append(baseConfig.getPlatform()).append("\",\n");
                currentPlatformConfig.append("      \"client_id\": \"").append(commonConfig.getKuaishouClientId()).append("\",\n");
                currentPlatformConfig.append("      \"client_secret\": \"").append(commonConfig.getKuaishouClientSecret()).append("\",\n");
                currentPlatformConfig.append("      \"product\": \"").append(baseConfig.getProduct()).append("\",\n");
                currentPlatformConfig.append("      \"customer\": \"").append(baseConfig.getCustomer()).append("\",\n");
                currentPlatformConfig.append("      \"appid\": \"").append(baseConfig.getAppid()).append("\",\n");
                currentPlatformConfig.append("      \"token_id\": ").append(baseConfig.getTokenId()).append(",\n");
                currentPlatformConfig.append("      \"version\": \"").append(baseConfig.getVersion()).append("\",\n");
                currentPlatformConfig.append("      \"cl\": \"").append(baseConfig.getCl()).append("\"\n");
            } else if ("weixin".equals(platform)) {
                currentPlatformConfig.append("      \"app_name\": \"").append(baseConfig.getAppName()).append("\",\n");
                currentPlatformConfig.append("      \"app_code\": \"").append(baseConfig.getAppCode()).append("\",\n");
                currentPlatformConfig.append("      \"product\": \"").append(baseConfig.getProduct()).append("\",\n");
                currentPlatformConfig.append("      \"customer\": \"").append(baseConfig.getCustomer()).append("\",\n");
                currentPlatformConfig.append("      \"appid\": \"").append(baseConfig.getAppid()).append("\",\n");
                currentPlatformConfig.append("      \"token_id\": ").append(baseConfig.getTokenId()).append(",\n");
                currentPlatformConfig.append("      \"version\": \"").append(baseConfig.getVersion()).append("\",\n");
                currentPlatformConfig.append("      \"cl\": \"").append(baseConfig.getCl()).append("\"\n");
            }
            
            String fileContent;
            // 如果文件已存在，读取并更新内容，否则创建新文件
            if (java.nio.file.Files.exists(configPath)) {
                String existingContent = new String(java.nio.file.Files.readAllBytes(configPath), java.nio.charset.StandardCharsets.UTF_8);
                fileContent = updateExistingBaseConfig(existingContent, platform, currentPlatformConfig.toString());
            } else {
                // 创建全新的配置文件
                StringBuilder sb = new StringBuilder();
                sb.append("export default {\n");
                sb.append("  AppConfig: {\n");

                // tt
                sb.append("    'tt': {");
                if ("douyin".equals(platform)) {
                    sb.append("\n");
                    sb.append(currentPlatformConfig.toString());
                }
                sb.append("    },\n");

                // ks
                sb.append("    'ks': {");
                if ("kuaishou".equals(platform)) {
                    sb.append("\n");
                    sb.append(currentPlatformConfig.toString());
                }
                sb.append("    },\n");

                // wx
                sb.append("    'wx': {");
                if ("weixin".equals(platform)) {
                    sb.append("\n");
                    sb.append(currentPlatformConfig.toString());
                }
                sb.append("    },\n");

                // bd
                sb.append("    'bd': {}\n");

                sb.append("  }\n");
                sb.append("}\n");
                fileContent = sb.toString();
            }
            
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
    
    /**
     * 更新现有的baseConfig配置文件，只更新指定平台的配置，保留其他平台的配置
     * @param existingContent 现有的配置文件内容
     * @param platform 当前平台
     * @param platformConfig 当前平台的配置内容
     * @return 更新后的完整配置文件内容
     */
    private String updateExistingBaseConfig(String existingContent, String platform, String platformConfig) {
        // 将平台名称映射到配置文件中的键名
        String platformKey = null;
        switch (platform) {
            case "douyin":
                platformKey = "'tt'";
                break;
            case "kuaishou":
                platformKey = "'ks'";
                break;
            case "weixin":
                platformKey = "'wx'";
                break;
            default:
                platformKey = "'bd'";
                break;
        }
        
        // 查找平台配置块的开始和结束位置
        String platformBlockStart = "    " + platformKey + ": {";
        int platformBlockStartIndex = existingContent.indexOf(platformBlockStart);
        
        if (platformBlockStartIndex != -1) {
            // 找到了平台配置块，更新它
            int platformBlockEndIndex = existingContent.indexOf("    }", platformBlockStartIndex + platformBlockStart.length());
            if (platformBlockEndIndex != -1) {
                // 找到平台配置块的结束位置
                platformBlockEndIndex += "    }".length();
                
                // 构造新的平台配置块
                StringBuilder newPlatformBlock = new StringBuilder();
                newPlatformBlock.append(platformBlockStart).append("\n");
                if (!platformConfig.isEmpty()) {
                    newPlatformBlock.append(platformConfig);
                }
                newPlatformBlock.append("    }");
                
                // 替换旧的平台配置块
                return existingContent.substring(0, platformBlockStartIndex) + 
                       newPlatformBlock.toString() + 
                       existingContent.substring(platformBlockEndIndex);
            }
        } else {
            // 没有找到平台配置块，需要添加新的平台配置
            // 查找bd配置块的位置，然后在它之前插入新的平台配置块
            String bdBlockStart = "    'bd': {}";
            int bdBlockStartIndex = existingContent.indexOf(bdBlockStart);
            
            if (bdBlockStartIndex != -1) {
                // 构造新的平台配置块
                StringBuilder newPlatformBlock = new StringBuilder();
                newPlatformBlock.append("    ").append(platformKey).append(": {").append("\n");
                if (!platformConfig.isEmpty()) {
                    newPlatformBlock.append(platformConfig);
                }
                newPlatformBlock.append("    },\n");
                
                // 在bd配置块之前插入新的平台配置块
                return existingContent.substring(0, bdBlockStartIndex) + 
                       newPlatformBlock.toString() + 
                       existingContent.substring(bdBlockStartIndex);
            }
        }
        
        // 如果以上方法都不行，使用后备方案
        return createFullBaseConfig(platform, platformConfig);
    }
    
    /**
     * 创建完整的baseConfig配置内容
     * @param platform 当前平台
     * @param platformConfig 当前平台的配置内容
     * @return 完整的配置文件内容
     */
    private String createFullBaseConfig(String platform, String platformConfig) {
        StringBuilder sb = new StringBuilder();
        sb.append("export default {\n");
        sb.append("  AppConfig: {\n");

        // tt
        sb.append("    'tt': {");
        if ("douyin".equals(platform)) {
            sb.append("\n").append(platformConfig);
        }
        sb.append("    },\n");

        // ks
        sb.append("    'ks': {");
        if ("kuaishou".equals(platform)) {
            sb.append("\n").append(platformConfig);
        }
        sb.append("    },\n");

        // wx
        sb.append("    'wx': {");
        if ("weixin".equals(platform)) {
            sb.append("\n").append(platformConfig);
        }
        sb.append("    },\n");

        // bd
        sb.append("    'bd': {}\n");

        sb.append("  }\n");
        sb.append("}\n");
        
        return sb.toString();
    }
    
    /**
     * 处理deliver配置
     */
    private void createDeliverConfigFile(String taskId, String buildCode, String platform, CreateNovelAppRequest.DeliverConfig deliverConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-4-2] 开始处理deliverConfig配置文件: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "deliverConfigs" + File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
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
            
            String fileContent;
            // 如果文件已存在，读取并更新内容，否则创建新文件
            if (java.nio.file.Files.exists(configPath)) {
                // 读取现有文件内容
                String existingContent = new String(java.nio.file.Files.readAllBytes(configPath), java.nio.charset.StandardCharsets.UTF_8);
                
                // 构造当前平台的配置
                java.util.LinkedHashMap<String, Object> platformConfigMap = new java.util.LinkedHashMap<>();
                platformConfigMap.put("deliver_id", deliverConfig != null && deliverConfig.getDeliverId() != null ? deliverConfig.getDeliverId() : "");
                platformConfigMap.put("banner_id", deliverConfig != null && deliverConfig.getBannerId() != null ? deliverConfig.getBannerId() : "");
                platformConfigMap.put("deliverAdId", "");
                platformConfigMap.put("bannerAdId", "");
                platformConfigMap.put("enable", true);
                platformConfigMap.put("useTest", false);
                
                // 更新现有配置
                fileContent = updateExistingDeliverConfig(existingContent, platform, platformConfigMap);
            } else {
                // 创建全新的配置文件
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
                fileContent = finalSb.toString();
            }
            
            java.nio.file.Files.write(configPath, fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-4-2] deliverConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, fileContent, CreateNovelLogType.INFO);
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
    
    /**
     * 更新现有的deliverConfig配置文件，只更新指定平台的配置，保留其他平台的配置
     * @param existingContent 现有的配置文件内容
     * @param platform 当前平台
     * @param platformConfigMap 当前平台的配置内容
     * @return 更新后的完整配置文件内容
     */
    private String updateExistingDeliverConfig(String existingContent, String platform, java.util.Map<String, Object> platformConfigMap) {
        try {
            // 将平台名称映射到配置文件中的键名
            String platformKey = platformToKey(platform);
            
            // 使用Jackson解析JSON配置
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonPart = existingContent.substring(existingContent.indexOf("export default ") + "export default ".length());
            if (jsonPart.endsWith(";")) {
                jsonPart = jsonPart.substring(0, jsonPart.length() - 1);
            }
            
            // 解析现有配置
            java.util.Map<String, Object> existingConfigMap = objectMapper.readValue(jsonPart, java.util.Map.class);
            
            // 更新指定平台的配置
            existingConfigMap.put(platformKey, platformConfigMap);
            
            // 重新生成配置文件内容
            StringBuilder finalSb = new StringBuilder();
            finalSb.append("export default ");
            finalSb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(existingConfigMap));
            finalSb.append(";\n");
            
            return finalSb.toString();
        } catch (Exception e) {
            // 如果解析失败，使用后备方案（创建全新的配置文件）
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
            
            // 更新当前平台配置
            String key = platformToKey(platform);
            if (deliverConfigMap.containsKey(key)) {
                deliverConfigMap.put(key, platformConfigMap);
            }
            
            // 生成最终内容
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            try {
                StringBuilder finalSb = new StringBuilder();
                finalSb.append("export default ");
                finalSb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deliverConfigMap));
                finalSb.append(";\n");
                return finalSb.toString();
            } catch (Exception ex) {
                throw new RuntimeException("无法生成deliver配置文件内容", ex);
            }
        }
    }
}
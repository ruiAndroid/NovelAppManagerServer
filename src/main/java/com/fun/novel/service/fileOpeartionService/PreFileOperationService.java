package com.fun.novel.service.fileOpeartionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.dto.CreateNovelLogType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 各种preBuild文件操作服务
 * 处理manifest.json、pages-xx.json、theme.less等基础配置文件
 * 
 * Note: This class is not annotated with @Service to avoid being registered as a Spring Bean.
 * It is intended to be used as a base class for other services.
 */
@Service
public class PreFileOperationService extends AbstractConfigFileOperationService{



    public void createPreFiles(String taskId,CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();


        createPrebuildBuildDir(taskId, buildCode, platform, baseConfig, rollbackActions, true);
        createDouyinPrefetchFile(taskId, buildCode, platform, rollbackActions, true);

    }
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
            Path destPath = Paths.get(destDir);
            // 只有当目录不存在时才创建，避免删除已有目录
            if (!Files.exists(destPath)) {
                Files.createDirectories(destPath);
                // 立即添加回滚动作，确保后续任何失败都能回滚buildCode目录
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除" + destDir, CreateNovelLogType.ERROR);
                        deleteDirectoryRecursively(destPath);
                    } catch (Exception ignore) {}
                });
            }
            
            // 只复制manifest.json和对应平台的pages-xx.json
            Path srcPath = Paths.get(srcDir);
            // manifest.json
            Path manifestSrc = srcPath.resolve("manifest.json");
            Path manifestDest = destPath.resolve("manifest.json");
            // 只有当目标文件不存在时才复制
            if (!Files.exists(manifestDest)) {
                Files.copy(manifestSrc, manifestDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                // manifest.json回滚动作：写入前先备份，写入后立即添加回滚
                String manifestBackup = manifestDest.toString() + ".bak";
                Files.copy(manifestDest, Paths.get(manifestBackup), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原manifest.json",CreateNovelLogType.ERROR);
                        Files.copy(Paths.get(manifestBackup), manifestDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(Paths.get(manifestBackup));
                    } catch (Exception ignore) {}
                });
            }
            
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
            // 只有当目标文件不存在时才复制
            if (!Files.exists(pagesDest)) {
                Files.copy(pagesSrc, pagesDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                // pages-xx.json回滚动作：写入前先备份，写入后立即添加回滚
                String pagesBackup = pagesDest.toString() + ".bak";
                Files.copy(pagesDest, Paths.get(pagesBackup), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原"+pagesFileName,CreateNovelLogType.ERROR);
                        Files.copy(Paths.get(pagesBackup), pagesDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(Paths.get(pagesBackup));
                    } catch (Exception ignore) {}
                });
            }
            
            // 2-1-2 编辑manifest.json内容
            if (withLogAndDelay) {
                taskLogger.log(taskId, "[2-1-2] 开始编辑manifest.json", CreateNovelLogType.PROCESSING);
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            try {
                taskLogger.log(taskId, "[2-1-2-1] 读取manifest.json", CreateNovelLogType.INFO);
                String manifestContent = new String(Files.readAllBytes(manifestDest), StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode manifestNode = (ObjectNode) objectMapper.readTree(manifestContent);
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
                            ((ObjectNode)manifestNode.get("mp-toutiao")).put("appid", baseConfig.getAppid());
                        }
                        break;
                    case "kuaishou":
                        if (manifestNode.has("mp-kuaishou")) {
                            ((ObjectNode)manifestNode.get("mp-kuaishou")).put("appid", baseConfig.getAppid());
                        }
                        break;
                    case "weixin":
                        if (manifestNode.has("mp-weixin")) {
                            ((ObjectNode)manifestNode.get("mp-weixin")).put("appid", baseConfig.getAppid());
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
                try { Files.deleteIfExists(Paths.get(manifestDest.toString() + ".bak")); } catch (Exception ignore) {}
            } catch (Exception e) {
                // 还原备份
                String manifestBackup = manifestDest.toString() + ".bak";
                taskLogger.log(taskId, "回滚动作：还原manifest.json",CreateNovelLogType.ERROR);
                Files.copy(Paths.get(manifestBackup), manifestDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(Paths.get(manifestBackup));
                throw new RuntimeException("manifest.json内容编辑失败: " + e.getMessage(), e);
            }
            //2-1-3 编辑pages-xx.json文件
            if (withLogAndDelay) {
                taskLogger.log(taskId, "[2-1-3] 开始编辑" + pagesFileName, CreateNovelLogType.PROCESSING);
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            try {
                taskLogger.log(taskId, "[2-1-3-1] 读取" + pagesFileName, CreateNovelLogType.INFO);
                String pagesContent = new String(Files.readAllBytes(pagesDest), StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(pagesContent);
                if (withLogAndDelay) {
                    try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                if (rootNode.has("pages") && rootNode.get("pages").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode pageNode : rootNode.get("pages")) {
                        if (!pageNode.has("path") || !pageNode.has("style")) continue;
                        String path = pageNode.get("path").asText();
                        ObjectNode styleNode = (ObjectNode) pageNode.get("style");
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
                try { Files.deleteIfExists(Paths.get(pagesDest.toString() + ".bak")); } catch (Exception ignore) {}
            } catch (Exception e) {
                // 还原备份
                String pagesBackup = pagesDest.toString() + ".bak";
                taskLogger.log(taskId, "回滚动作：还原"+pagesFileName,CreateNovelLogType.ERROR);
                Files.copy(Paths.get(pagesBackup), pagesDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(Paths.get(pagesBackup));
                throw new RuntimeException(pagesFileName+"内容编辑失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            taskLogger.log(taskId, "[2-1]目录prebuild/build处理失败: " + e.getMessage(), CreateNovelLogType.ERROR);
            // 回滚自身
            try {
                taskLogger.log(taskId, "回滚动作：删除" + destDir, CreateNovelLogType.ERROR);
                Path destPath = Paths.get(destDir);
                // 只有当目录是我们创建的才删除
                if (!Files.exists(destPath)) {
                    deleteDirectoryRecursively(destPath);
                }
            } catch (Exception ignore) {}
            throw new RuntimeException("目录prebuild/build处理失败: " + e.getMessage(), e);
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
        Path srcPath = Paths.get(srcFile);
        Path destPath = Paths.get(destFile);
        String backupPath = destFile + ".bak";
        try {
            // 复制原文件
            Files.copy(srcPath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // 立即添加回滚动作
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：删除" + destFile, CreateNovelLogType.ERROR);
                    Files.deleteIfExists(destPath);
                } catch (Exception ignore) {}
            });
            // 备份一份用于内容回滚
            Files.copy(destPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：还原" + destFile, CreateNovelLogType.ERROR);
                    Files.copy(Paths.get(backupPath), destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(Paths.get(backupPath));
                } catch (Exception ignore) {}
            });
            taskLogger.log(taskId, "[2-3-1] 复制prelaunch-fun.js完成", CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 读取内容并修改brand
            String content = new String(Files.readAllBytes(destPath), StandardCharsets.UTF_8);
            // 简单正则替换 param.brand = 'xxx' 或 param.brand: 'xxx'
            String newContent = content.replaceAll("(brand\\s*:\\s*)['\"][^'\"]+['\"]", "$1'" + buildCode + "'");
            Files.write(destPath, newContent.getBytes(StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-3-2] 修改param.brand为" + buildCode + "完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, newContent, CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除prelaunch.bak
            try { Files.deleteIfExists(Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { Files.deleteIfExists(destPath); } catch (Exception ignore) {}
            throw new RuntimeException("抖音预取文件处理失败: " + e.getMessage(), e);
        }
    }



    /**
     * 删除预编译目录
     * @param buildCode
     * @param platform
     * @param baseConfig
     * @param rollbackActions
     */
    private void deletePrebuildBuildDir(String buildCode, String platform, CreateNovelAppRequest.BaseConfig baseConfig, List<Runnable> rollbackActions) {
        String prebuildDir = buildWorkPath + File.separator + "prebuild" + File.separator + "build";
        String destDir = prebuildDir + File.separator + buildCode;


    }


    public void deletePreFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        //如果是抖音，删除prefetch/目录下的prelaunch-xx.js文件
        if (!"douyin".equals(platform)) return;
        
        String prefetchDir = buildWorkPath + File.separator + "prefetchbuild";
        String destFile = prefetchDir + File.separator + "prelaunch-" + buildCode + ".js";
        Path destPath = Paths.get(destFile);
        String backupPath = destFile + ".bak";
        
        try {
            // 只有当文件存在时才删除，并添加回滚动作
            if (Files.exists(destPath)) {
                // 备份文件用于回滚
                Files.copy(destPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // 删除文件
                Files.deleteIfExists(destPath);
                
                // 添加回滚动作
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(null, "回滚动作：还原" + destFile, CreateNovelLogType.ERROR);
                        Files.copy(Paths.get(backupPath), destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
                
                // 操作成功后删除备份文件
                Files.deleteIfExists(Paths.get(backupPath));
            }
        } catch (Exception e) {
            // 如果出现异常，确保清理备份文件
            try { Files.deleteIfExists(Paths.get(backupPath)); } catch (Exception ignore) {}
            throw new RuntimeException("删除抖音预取文件失败: " + e.getMessage(), e);
        }
    }
}
package com.fun.novel.service.impl;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppResourceFileService;
import com.fun.novel.utils.CreateNovelTaskLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.util.List;

@Service
public class NovelAppResourceFileServiceImpl implements NovelAppResourceFileService {

    @Autowired
    private CreateNovelTaskLogger taskLogger;
    
    @Value("${build.workPath}")
    private String buildWorkPath;

    private static final int RESOURCE_STEP_DELAY_MS = 1500;

    @Override
    public void processAllResourceFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        String imgDir = buildWorkPath + File.separator + "src" + File.separator + "static";
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        String buildCode = commonConfig.getBuildCode();
        String imgSrcDir = imgDir + File.separator + "img-xingchen";
        String imgDestDir = imgDir + File.separator + "img-" + buildCode;
        java.nio.file.Path destPath = java.nio.file.Paths.get(imgDestDir);
        java.nio.file.Path srcPath = java.nio.file.Paths.get(imgSrcDir);
        taskLogger.log(taskId, "[3-1] 图片资源文件处理：准备生成目录 " + imgDestDir, com.fun.novel.dto.CreateNovelLogType.PROCESSING);
        try { Thread.sleep(RESOURCE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        try {
            // 如果目标目录已存在，先删除
            if (java.nio.file.Files.exists(destPath)) {
                deleteDirectoryRecursively(destPath);
            }
            java.nio.file.Files.createDirectories(destPath);
            // 回滚动作：删除新建的img-xx目录
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：删除" + imgDestDir, com.fun.novel.dto.CreateNovelLogType.ERROR);
                    deleteDirectoryRecursively(destPath);
                } catch (Exception ignore) {}
            });
            taskLogger.log(taskId, "[3-1] 图片资源文件目录 " + imgDestDir + " 创建完成", com.fun.novel.dto.CreateNovelLogType.SUCCESS);
            try { Thread.sleep(RESOURCE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            // 复制img-xingchen下所有文件到img-xx（递归复制，支持子目录）
            java.io.File srcDirFile = new java.io.File(imgSrcDir);
            if (!srcDirFile.exists() || !srcDirFile.isDirectory()) {
                throw new RuntimeException("图片资源文件目录不存在: " + imgSrcDir);
            }
            copyDirectoryRecursively(srcPath, destPath);
            taskLogger.log(taskId, "[3-2] 图片资源文件复制完成: " + imgSrcDir + " -> " + imgDestDir, com.fun.novel.dto.CreateNovelLogType.SUCCESS);
            try { Thread.sleep(RESOURCE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

            // 复制完成后，处理SVG主题色
            CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
            String mainTheme = baseConfig != null ? baseConfig.getMainTheme() : null;
            if (mainTheme != null && !mainTheme.isEmpty()) {
                String errorSvgPath = imgDestDir + File.separator + "error" + File.separator + "error.svg";
                String emptySvgPath = imgDestDir + File.separator + "error" + File.separator + "empty.svg";
                processSvgFill(errorSvgPath, "id=\"缺省页/空\"", mainTheme, rollbackActions, taskId, "error.svg");
                processSvgFill(emptySvgPath, "id=\"合并\"", mainTheme, rollbackActions, taskId, "empty.svg");
                // 处理mine目录下svg文件
                String mineDir = imgDestDir + File.separator + "mine";
                processSvgFill(mineDir + File.separator + "mine_charge_icon.svg", "id=\"编组\"", mainTheme, rollbackActions, taskId, "mine_charge_icon.svg");
                processSvgFill(mineDir + File.separator + "mine_coin_record.svg", "id=\"编组-2\"", mainTheme, rollbackActions, taskId, "mine_coin_record.svg");
                processSvgFill(mineDir + File.separator + "mine_contact_icon.svg", "id=\"编组-2\"", mainTheme, rollbackActions, taskId, "mine_contact_icon.svg");
                processSvgFill(mineDir + File.separator + "mine_message_subscribe.svg", "id=\"我的-订阅关\"", mainTheme, rollbackActions, taskId, "mine_message_subscribe.svg");
                processSvgFill(mineDir + File.separator + "mine_purchase_record.svg", "id=\"编组-2\"", mainTheme, rollbackActions, taskId, "mine_purchase_record.svg");
                processSvgFill(mineDir + File.separator + "mine_set_icon.svg", "id=\"我的/设置-48\"", mainTheme, rollbackActions, taskId, "mine_set_icon.svg");
            } else {
                taskLogger.log(taskId, "未获取到主题色mainTheme，跳过SVG主题色替换", com.fun.novel.dto.CreateNovelLogType.INFO);
            }
        } catch (Exception e) {
            taskLogger.log(taskId, "[3-1/3-2] 图片资源文件处理失败: " + e.getMessage(), com.fun.novel.dto.CreateNovelLogType.ERROR);
            // 回滚自身
            try {
                taskLogger.log(taskId, "图片资源文件 回滚动作：删除" + imgDestDir, com.fun.novel.dto.CreateNovelLogType.ERROR);
                deleteDirectoryRecursively(destPath);
            } catch (Exception ignore) {}
            throw new RuntimeException("图片资源文件处理失败: " + e.getMessage(), e);
        }
    }

    // 工具方法：递归删除目录
    private void deleteDirectoryRecursively(java.nio.file.Path path) throws java.io.IOException {
        if (java.nio.file.Files.notExists(path)) return;
        java.nio.file.Files.walk(path)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try { java.nio.file.Files.deleteIfExists(p); } catch (java.io.IOException ignore) {}
                });
    }

    // 工具方法：递归复制目录及文件
    private void copyDirectoryRecursively(java.nio.file.Path source, java.nio.file.Path target) throws java.io.IOException {
        if (java.nio.file.Files.isDirectory(source)) {
            if (java.nio.file.Files.notExists(target)) {
                java.nio.file.Files.createDirectories(target);
            }
            try (java.nio.file.DirectoryStream<java.nio.file.Path> entries = java.nio.file.Files.newDirectoryStream(source)) {
                for (java.nio.file.Path entry : entries) {
                    copyDirectoryRecursively(entry, target.resolve(entry.getFileName()));
                }
            }
        } else {
            java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // 新增方法：处理SVG主题色替换
    private void processSvgFill(String svgPath, String idSelector, String mainTheme, List<Runnable> rollbackActions, String taskId, String fileDesc) {
        File svgFile = new File(svgPath);
        if (!svgFile.exists()) {
            taskLogger.log(taskId, fileDesc + " 不存在，跳过主题色替换", com.fun.novel.dto.CreateNovelLogType.INFO);
            return;
        }
        try {
            String content = java.nio.file.Files.readString(svgFile.toPath());
            // 备份原内容
            rollbackActions.add(() -> {
                try { java.nio.file.Files.writeString(svgFile.toPath(), content); } catch (Exception ignore) {}
            });
            String newContent = content;
            if ("error.svg".equals(fileDesc)) {
                // 替换id="缺省页/空"的g标签的fill属性为主题色
                newContent = newContent.replaceAll(
                    "(<g[^>]*id=\\\"缺省页/空\\\"[^>]*fill=\\\")#[A-Fa-f0-9]{6,8}(\\\"[^>]*>)",
                    "$1" + mainTheme + "$2"
                );
            } else if ("empty.svg".equals(fileDesc)) {
                // 替换id="合并"的path标签的fill属性为主题色
                newContent = newContent.replaceAll(
                    "(<path[^>]*id=\\\"合并\\\"[^>]*fill=\\\")#[A-Fa-f0-9]{6,8}(\\\"[^>]*>)",
                    "$1" + mainTheme + "$2"
                );
            } else if ("mine_charge_icon.svg".equals(fileDesc)) {
                // 替换id="编组"的g标签的fill属性为主题色
                newContent = newContent.replaceAll(
                    "(<g[^>]*id=\\\"编组\\\"[^>]*fill=\\\")#[A-Fa-f0-9]{6,8}(\\\"[^>]*>)",
                    "$1" + mainTheme + "$2"
                );
            } else if ("mine_coin_record.svg".equals(fileDesc) || "mine_contact_icon.svg".equals(fileDesc) || "mine_purchase_record.svg".equals(fileDesc)) {
                // 替换id="编组-2"的g标签的fill属性为主题色
                newContent = newContent.replaceAll(
                    "(<g[^>]*id=\\\"编组-2\\\"[^>]*fill=\\\")#[A-Fa-f0-9]{6,8}(\\\"[^>]*>)",
                    "$1" + mainTheme + "$2"
                );
            } else if ("mine_message_subscribe.svg".equals(fileDesc)) {
                // 替换id="我的-订阅关"的g标签的fill属性为主题色
                newContent = newContent.replaceAll(
                    "(<g[^>]*id=\\\"我的-订阅关\\\"[^>]*fill=\\\")#[A-Fa-f0-9]{6,8}(\\\"[^>]*>)",
                    "$1" + mainTheme + "$2"
                );
            } else if ("mine_set_icon.svg".equals(fileDesc)) {
                // 替换id="我的/设置-48"的g标签的fill属性为主题色
                newContent = newContent.replaceAll(
                    "(<g[^>]*id=\\\"我的/设置-48\\\"[^>]*fill=\\\")#[A-Fa-f0-9]{6,8}(\\\"[^>]*>)",
                    "$1" + mainTheme + "$2"
                );
            }
            java.nio.file.Files.writeString(svgFile.toPath(), newContent);
            taskLogger.log(taskId, fileDesc + " 主题色替换完成", com.fun.novel.dto.CreateNovelLogType.SUCCESS);
        } catch (Exception e) {
            taskLogger.log(taskId, fileDesc + " 主题色替换失败: " + e.getMessage(), com.fun.novel.dto.CreateNovelLogType.ERROR);
            throw new RuntimeException(fileDesc + " 主题色替换失败: " + e.getMessage(), e);
        }
    }
} 
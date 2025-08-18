package com.fun.novel.service.fileOpeartionService;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppLocalFileOperationService;
import com.fun.novel.dto.CreateNovelLogType;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 应用配置文件操作服务
 * 处理AppConfig.js和package.json等应用级配置文件
 */
@Service
public class AppConfigFileOperationService extends AbstractConfigFileOperationService{


    public void updateAppConfigAndPackageFile(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        updateAppConfigFile(taskId, buildCode, rollbackActions,true);
        updatePackageJsonFile(taskId, buildCode, platform, rollbackActions,true);
    }

    /**
     * 处理AppConfig.js文件
     */
    private void updateAppConfigFile(String taskId, String buildCode, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-5] 开始处理AppConfig.js: " + buildWorkPath + java.io.File.separator + "src" + java.io.File.separator + "modules" + java.io.File.separator + "mod_config" + java.io.File.separator + "AppConfig.js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String configDir = buildWorkPath + java.io.File.separator + "src" + java.io.File.separator + "modules" + java.io.File.separator + "mod_config";
        String configFile = configDir + java.io.File.separator + "AppConfig.js";
        Path configPath = Paths.get(configFile);
        String backupPath = configFile + ".bak";
        try {
            // 备份原文件
            if (Files.exists(configPath)) {
                Files.copy(configPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原AppConfig.js",CreateNovelLogType.ERROR);
                        Files.copy(Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                // 文件不存在，回滚时删除
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除AppConfig.js",CreateNovelLogType.ERROR);
                        Files.deleteIfExists(configPath);
                    } catch (Exception ignore) {}
                });
            }
            // 读取原内容
            String content = Files.exists(configPath) ? new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8) : "";
            List<String> lines = new java.util.ArrayList<>();
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
            Files.write(configPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-5] AppConfig.js写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, sb.toString(), CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            deleteBackupFile(backupPath);
        } catch (Exception e) {
            // 还原自身
            try { Files.deleteIfExists(configPath); } catch (Exception ignore) {}
            throw new RuntimeException("AppConfig.js处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理package.json文件
     */
    private void updatePackageJsonFile(String taskId, String buildCode, String platform, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-6] 开始处理package.json: " + buildWorkPath + java.io.File.separator + "package.json", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String packageJsonPath = buildWorkPath + java.io.File.separator + "package.json";
        Path jsonPath = Paths.get(packageJsonPath);
        String backupPath = packageJsonPath + ".bak";
        try {
            // 备份原文件
            if (Files.exists(jsonPath)) {
                Files.copy(jsonPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：还原package.json",CreateNovelLogType.ERROR);
                        Files.copy(Paths.get(backupPath), jsonPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
            } else {
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除package.json",CreateNovelLogType.ERROR);
                        Files.deleteIfExists(jsonPath);
                    } catch (Exception ignore) {}
                });
            }
            // 读取并解析JSON
            String content = Files.exists(jsonPath) ? new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8) : "{}";
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
            Files.write(jsonPath, finalContent.getBytes(StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-6] package.json写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, finalContent, CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            deleteBackupFile(backupPath);
        } catch (Exception e) {
            // 还原自身
            try { Files.deleteIfExists(jsonPath); } catch (Exception ignore) {}
            throw new RuntimeException("package.json处理失败: " + e.getMessage(), e);
        }
    }
}
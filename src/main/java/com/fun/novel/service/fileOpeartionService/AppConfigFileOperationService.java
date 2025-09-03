package com.fun.novel.service.fileOpeartionService;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.dto.CreateNovelLogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
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

    private static final Logger log = LoggerFactory.getLogger(AppConfigFileOperationService.class);

    public void updateAppConfigAndPackageFile(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        updateAppConfigFile(taskId, buildCode, rollbackActions,true);
        updatePackageJsonFile(taskId, buildCode, platform, rollbackActions,true);
    }

    public void deleteAppConfigAndPackageFile(CreateNovelAppRequest params, List<Runnable> rollbackActions,boolean isLast){
        deleteAppConfigFile(params, rollbackActions,isLast);
        deletePackageConfigFile(params, rollbackActions,isLast);
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




    public void deleteAppConfigFile(CreateNovelAppRequest params, List<Runnable> rollbackActions, boolean isLast) {

        if(!isLast){

            log.warn("删除AppConfig文件，当前还存在其他平台小程序，不需要删除" );
            return;
        }
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        String appConfigDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config";
        String appConfigFile = appConfigDir + File.separator + "AppConfig.js";
        java.nio.file.Path appConfigPath = java.nio.file.Paths.get(appConfigFile);

        try {
            // 检查文件是否存在
            if (!java.nio.file.Files.exists(appConfigPath)) {
                log.warn("AppConfig文件不存在: {}", appConfigFile);
                return;
            }

            // 读取原内容
            java.util.List<String> lines = java.nio.file.Files.readAllLines(appConfigPath, java.nio.charset.StandardCharsets.UTF_8);

            // 备份原文件
            String backupPath = appConfigFile + ".bak";
            java.nio.file.Files.copy(appConfigPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // 添加回滚动作
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(null, "回滚动作：还原AppConfig文件", CreateNovelLogType.ERROR);
                    java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), appConfigPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                } catch (Exception ignore) {}
            });

            // 构造要删除的导入行模式
            String importConfigLine = "import " + buildCode + "Config from './baseConfigs/" + buildCode + "';";
            String importPayConfigLine = "import " + buildCode + "PayConfig from './payConfigs/" + buildCode + "';";
            String importAdConfigLine = "import " + buildCode + "AdConfig from './adConfigs/" + buildCode + "';";
            String importDeliverConfigLine = "import " + buildCode + "DeliverConfig from './deliverConfigs/" + buildCode + "';";
            String importCommonConfigLine = "import " + buildCode + "CommonConfig from './commonConfigs/" + buildCode + "';";

            // 构造getBrand方法中要删除的内容模式
            String brandCaseStart = "// #ifdef MP-" + buildCode.toUpperCase();
            String brandCaseReturn = "return '" + buildCode + "'";
            String brandCaseEnd = "// #endif";

            // 构造loadPayConfig方法中要删除的内容模式
            String payConfigCase = "case '" + buildCode + "':";
            String payConfigReturn = "return " + buildCode + "PayConfig;";

            // 构造loadBrandConfig方法中要删除的内容模式
            String brandConfigCase = "case '" + buildCode + "':";
            String brandConfigReturn = "return " + buildCode + "Config;";

            // 构造loadAdConfig方法中要删除的内容模式
            String adConfigCase = "case '" + buildCode + "':";
            String adConfigReturn = "return " + buildCode + "AdConfig;";

            // 构造loadDeliverConfig方法中要删除的内容模式
            String deliverConfigCase = "case '" + buildCode + "':";
            String deliverConfigReturn = "return " + buildCode + "DeliverConfig;";

            // 构造loadCommonConfig方法中要删除的内容模式
            String commonConfigCase = "case '" + buildCode + "':";
            String commonConfigReturn = "return " + buildCode + "CommonConfig;";

            // 过滤掉包含构建代码的行
            java.util.List<String> newLines = new java.util.ArrayList<>();
            boolean inBrandCaseBlock = false; // 用于跟踪是否在getBrand的case块中

            for (String line : lines) {
                String trimmedLine = line.trim();

                // 检查是否是需要删除的导入行
                if (trimmedLine.equals(importConfigLine) ||
                        trimmedLine.equals(importPayConfigLine) ||
                        trimmedLine.equals(importAdConfigLine) ||
                        trimmedLine.equals(importDeliverConfigLine) ||
                        trimmedLine.equals(importCommonConfigLine)) {
                    continue; // 跳过这些行
                }

                // 检查getBrand方法中的条件编译块
                if (trimmedLine.equals(brandCaseStart)) {
                    inBrandCaseBlock = true;
                    continue; // 跳过开始标记行
                }
                if (inBrandCaseBlock && trimmedLine.equals(brandCaseReturn)) {
                    continue; // 跳过return语句行
                }
                if (inBrandCaseBlock && trimmedLine.equals(brandCaseEnd)) {
                    inBrandCaseBlock = false;
                    continue; // 跳过结束标记行
                }

                // 检查各load方法中的case语句
                if (trimmedLine.equals(payConfigCase) ||
                        trimmedLine.equals(payConfigReturn) ||
                        trimmedLine.equals(brandConfigCase) ||
                        trimmedLine.equals(brandConfigReturn) ||
                        trimmedLine.equals(adConfigCase) ||
                        trimmedLine.equals(adConfigReturn) ||
                        trimmedLine.equals(deliverConfigCase) ||
                        trimmedLine.equals(deliverConfigReturn) ||
                        trimmedLine.equals(commonConfigCase) ||
                        trimmedLine.equals(commonConfigReturn)) {
                    continue; // 跳过这些行
                }

                // 保留其他所有行
                newLines.add(line);
            }

            // 写入新内容
            java.nio.file.Files.write(appConfigPath, newLines, java.nio.charset.StandardCharsets.UTF_8);

            // 操作成功后删除备份文件
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));

            log.info("成功删除AppConfig文件中的构建代码配置: {}", buildCode);
        } catch (Exception e) {
            log.error("删除AppConfig文件配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除AppConfig文件配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除package.json文件中的构建代码配置
     */
    public void deletePackageConfigFile(CreateNovelAppRequest params, List<Runnable> rollbackActions, boolean isLast) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        String packageFile = buildWorkPath + File.separator + "package.json";
        java.nio.file.Path packagePath = java.nio.file.Paths.get(packageFile);
        
        try {
            // 检查文件是否存在
            if (!java.nio.file.Files.exists(packagePath)) {
                log.warn("package.json文件不存在: {}", packageFile);
                return;
            }

            // 读取原内容
            String content = new String(java.nio.file.Files.readAllBytes(packagePath), java.nio.charset.StandardCharsets.UTF_8);
            
            // 备份原文件
            String backupPath = packageFile + ".bak";
            java.nio.file.Files.copy(packagePath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // 添加回滚动作
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(null, "回滚动作：还原package.json文件", CreateNovelLogType.ERROR);
                    java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), packagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                } catch (Exception ignore) {}
            });
            
            // 使用Jackson解析JSON配置
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // 配置ObjectMapper以允许单引号
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            // 配置ObjectMapper以允许不带引号的字段名
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            // 配置ObjectMapper以允许注释
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, true);
            // 配置ObjectMapper以允许尾随逗号
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
            // 配置ObjectMapper以允许YAML注释样式
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
            
            // 解析现有配置
            com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(content);
            
            // 构造要删除的键名
            String platformKey = platformToKey(platform);
            String scriptKey = platformKey + "-" + buildCode;
            String devKey = "dev:" + scriptKey;
            String buildKey = "build:" + scriptKey;
            
            // 删除scripts块下的内容
            if (root.has("scripts") && root.get("scripts").isObject()) {
                com.fasterxml.jackson.databind.node.ObjectNode scripts = (com.fasterxml.jackson.databind.node.ObjectNode) root.get("scripts");
                scripts.remove(devKey);
                scripts.remove(buildKey);
            }
            
            // 删除uni-app块下的scripts块内容
            if (root.has("uni-app") && root.get("uni-app").isObject()) {
                com.fasterxml.jackson.databind.node.ObjectNode uniApp = (com.fasterxml.jackson.databind.node.ObjectNode) root.get("uni-app");
                if (uniApp.has("scripts") && uniApp.get("scripts").isObject()) {
                    com.fasterxml.jackson.databind.node.ObjectNode uniAppScripts = (com.fasterxml.jackson.databind.node.ObjectNode) uniApp.get("scripts");
                    uniAppScripts.remove(scriptKey);
                }
            }
            
            // 写回文件
            String finalContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            java.nio.file.Files.write(packagePath, finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 操作成功后删除备份文件
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));

            log.info("成功删除package.json文件中的构建代码配置: {}", buildCode);
        } catch (Exception e) {
            log.error("删除package.json文件配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除package.json文件配置失败: " + e.getMessage(), e);
        }
    }
}
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

        createThemeFile(taskId, buildCode, baseConfig, rollbackActions, true);
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
                currentPlatformConfig.append("      app_name: \"").append(baseConfig.getAppName()).append("\",\n");
                currentPlatformConfig.append("      app_code: \"").append(baseConfig.getAppCode()).append("\",\n");
                currentPlatformConfig.append("      code: \"").append("toutiao").append("\",\n");
                currentPlatformConfig.append("      product: \"").append(baseConfig.getProduct()).append("\",\n");
                currentPlatformConfig.append("      customer: \"").append(baseConfig.getCustomer()).append("\",\n");
                currentPlatformConfig.append("      appid: \"").append(baseConfig.getAppid()).append("\",\n");
                currentPlatformConfig.append("      token_id: ").append(baseConfig.getTokenId()).append(",\n");
                currentPlatformConfig.append("      version: \"").append(baseConfig.getVersion()).append("\",\n");
                currentPlatformConfig.append("      cl: \"").append(baseConfig.getCl()).append("\"\n");
            } else if ("kuaishou".equals(platform)) {
                currentPlatformConfig.append("      app_name: \"").append(baseConfig.getAppName()).append("\",\n");
                currentPlatformConfig.append("      app_code: \"").append(baseConfig.getAppCode()).append("\",\n");
                currentPlatformConfig.append("      code: \"").append(baseConfig.getPlatform()).append("\",\n");
                currentPlatformConfig.append("      client_id: \"").append(commonConfig.getKuaishouClientId()).append("\",\n");
                currentPlatformConfig.append("      client_secret: \"").append(commonConfig.getKuaishouClientSecret()).append("\",\n");
                currentPlatformConfig.append("      product: \"").append(baseConfig.getProduct()).append("\",\n");
                currentPlatformConfig.append("      customer: \"").append(baseConfig.getCustomer()).append("\",\n");
                currentPlatformConfig.append("      appid: \"").append(baseConfig.getAppid()).append("\",\n");
                currentPlatformConfig.append("      token_id: ").append(baseConfig.getTokenId()).append(",\n");
                currentPlatformConfig.append("      version: \"").append(baseConfig.getVersion()).append("\",\n");
                currentPlatformConfig.append("      cl: \"").append(baseConfig.getCl()).append("\"\n");
            } else if ("weixin".equals(platform)) {
                currentPlatformConfig.append("      app_name: \"").append(baseConfig.getAppName()).append("\",\n");
                currentPlatformConfig.append("      app_code: \"").append(baseConfig.getAppCode()).append("\",\n");
                currentPlatformConfig.append("      product: \"").append(baseConfig.getProduct()).append("\",\n");
                currentPlatformConfig.append("      customer: \"").append(baseConfig.getCustomer()).append("\",\n");
                currentPlatformConfig.append("      appid: \"").append(baseConfig.getAppid()).append("\",\n");
                currentPlatformConfig.append("      token_id: ").append(baseConfig.getTokenId()).append(",\n");
                currentPlatformConfig.append("      version: \"").append(baseConfig.getVersion()).append("\",\n");
                currentPlatformConfig.append("      cl: \"").append(baseConfig.getCl()).append("\"\n");
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
                String platformKey = platformToKey(platform);
                
                // 只在对应平台生成详细配置，其他平台只保留空对象
                for (String pf : platforms) {
                    java.util.LinkedHashMap<String, Object> pfMap = new java.util.LinkedHashMap<>();
                    if (pf.equals(platformKey)) {
                        // 为当前平台生成详细配置
                        pfMap.put("deliver_id", "");
                        pfMap.put("banner_id", "");
                        pfMap.put("deliverAdId", "");
                        pfMap.put("bannerAdId", "");
                        pfMap.put("enable", true);
                        pfMap.put("useTest", false);
                    }
                    // 其他平台保留空对象
                    deliverConfigMap.put(pf, pfMap);
                }
                
                // 根据platform写入对应数据
                if (deliverConfig != null && deliverConfigMap.containsKey(platformKey)) {
                    java.util.Map<String, Object> pfMap = (java.util.Map<String, Object>) deliverConfigMap.get(platformKey);
                    pfMap.put("deliver_id", deliverConfig.getDeliverId() != null ? deliverConfig.getDeliverId() : "");
                    pfMap.put("banner_id", deliverConfig.getBannerId() != null ? deliverConfig.getBannerId() : "");
                }
                // 生成最终内容
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                StringBuilder finalSb = new StringBuilder();
                finalSb.append("export default ");
                String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deliverConfigMap);
                // 统一使用带单引号的外层键名并修复缩进
                jsonString = formatJsonString(jsonString);
                finalSb.append(jsonString);
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
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(existingConfigMap);
            // 统一使用带单引号的外层键名并修复缩进
            jsonString = formatJsonString(jsonString);
            finalSb.append(jsonString);
            finalSb.append(";\n");
            
            return finalSb.toString();
        } catch (Exception e) {
            taskLogger.log(null, "解析现有deliverConfig文件失败: " + e.getMessage(), CreateNovelLogType.ERROR);
            throw new RuntimeException("无法解析现有deliver配置文件内容: " + e.getMessage(), e);
        }
    }
    
    /**
     * 格式化JSON字符串，外层键使用单引号，内层键不使用引号并修复缩进
     * @param jsonString JSON字符串
     * @return 格式化后的字符串
     */
    private static String formatJsonString(String jsonString) {
        // 将带引号的键名替换为不带引号的键名（用于内层键）
        jsonString = jsonString.replaceAll("\"([a-zA-Z_][a-zA-Z0-9_]*)\"\\s*:", "$1:");
        
        // 为外层键添加单引号 - 处理所有平台键
        jsonString = jsonString.replaceAll("\"(tt|ks|wx|bd)\"\\s*:", "'$1':");
        jsonString = jsonString.replaceAll("(\\{|,\\s+)\\b(tt|ks|wx|bd)\\b\\s*:", "$1'$2':");
        // 特殊处理第一个键（对象开始后的第一个键）
        jsonString = jsonString.replaceAll("\\{\\s*(tt|ks|wx|bd)\\s*:", "{'$1':");
        
        // 创建格式化的结果
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        String[] lines = jsonString.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 判断是否是结束括号
            if (line.startsWith("}") || line.startsWith("]")) {
                indentLevel--;
            }
            
            // 添加缩进
            for (int i = 0; i < indentLevel; i++) {
                result.append("    ");
            }
            
            result.append(line).append("\n");
            
            // 判断是否是开始括号
            if (line.endsWith("{") || line.endsWith("[")) {
                indentLevel++;
            }
            
            // 处理同时包含开始和结束括号的情况
            if (line.contains("{") && line.contains("}")) {
                // 同一行包含开始和结束，不改变缩进级别
            }
        }
        
        // 返回结果，去掉最后的换行符
        String formatted = result.toString();
        if (formatted.endsWith("\n")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        
        return formatted;
    }
    
    /**
     * 删除指定appId的baseConfig配置
     *
     */
    public void deleteBaseConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

//        deleteThemeFile(buildCode, baseConfig, rollbackActions, false);
//        deleteDouyinPrefetchFile(buildCode, platform, rollbackActions, false);
//        deleteBaseConfigFile(buildCode, platform, baseConfig, commonConfig, rollbackActions, false);
//        deleteDeliverConfigFile(buildCode, platform, params.getDeliverConfig(),rollbackActions, false);

    }

}
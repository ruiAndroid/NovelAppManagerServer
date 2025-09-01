package com.fun.novel.service.fileOpeartionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppLocalFileOperationService;
import com.fun.novel.dto.CreateNovelLogType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 通用配置文件操作服务
 * 处理commonConfigs目录下的通用配置文件
 */
@Service
public class CommonConfigFileOperationService extends AbstractConfigFileOperationService{


    public void createCommonConfigLocalCodeFiles(String taskId,CreateNovelAppRequest params, List<Runnable> rollbackActions){
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        createCommonConfigFile(taskId, buildCode, platform, commonConfig, rollbackActions,true);
    }

    public void updateCommonConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions){
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        createCommonConfigFile(null, buildCode, platform, commonConfig, rollbackActions,false);
    }

    /**
     * 处理通用配置文件
     */
    private void createCommonConfigFile(String taskId, String buildCode, String platform,
                                       CreateNovelAppRequest.CommonConfig commonConfig, 
                                       List<Runnable> rollbackActions, boolean withLogAndDelay) {

        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-4-5] 开始处理commonConfig配置文件: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "commonConfigs" + File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
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
            
            String fileContent;
            // 如果文件已存在，读取并更新内容，否则创建新文件
            if (java.nio.file.Files.exists(configPath)) {
                // 读取现有文件内容
                String existingContent = new String(java.nio.file.Files.readAllBytes(configPath), java.nio.charset.StandardCharsets.UTF_8);
                
                // 构造当前平台的通用配置
                java.util.LinkedHashMap<String, Object> platformCommonConfigMap = new java.util.LinkedHashMap<>();
                java.util.LinkedHashMap<String, Object> homeCard = new java.util.LinkedHashMap<>();
                homeCard.put("style", 1);
                java.util.LinkedHashMap<String, Object> payCard = new java.util.LinkedHashMap<>();
                payCard.put("style", 1);
                java.util.LinkedHashMap<String, Object> loginType = new java.util.LinkedHashMap<>();
                loginType.put("mine", "anonymousLogin");
                loginType.put("reader", "anonymousLogin");
                platformCommonConfigMap.put("homeCard", homeCard);
                platformCommonConfigMap.put("payCard", payCard);
                platformCommonConfigMap.put("loginType", loginType);
                platformCommonConfigMap.put("contact", "");
                // iaaMode 默认
                java.util.LinkedHashMap<String, Object> iaaModeObj = new java.util.LinkedHashMap<>();
                iaaModeObj.put("enable", false);
                iaaModeObj.put("dialogStyle", 2);
                platformCommonConfigMap.put("iaaMode", iaaModeObj);
                platformCommonConfigMap.put("hidePayEntry", false);
                
                // 只对当前platform赋值
                String key = platformToKey(platform);
                if (commonConfig != null) {
                    // homeCard.style
                    homeCard.put("style", commonConfig.getHomeCardStyle() != null ? commonConfig.getHomeCardStyle() : 1);
                    // payCard.style
                    payCard.put("style", commonConfig.getPayCardStyle() != null ? commonConfig.getPayCardStyle() : 1);
                    // loginType
                    loginType.put("mine", commonConfig.getMineLoginType() != null ? commonConfig.getMineLoginType() : "anonymousLogin");
                    loginType.put("reader", commonConfig.getReaderLoginType() != null ? commonConfig.getReaderLoginType() : "anonymousLogin");
                    // contact
                    platformCommonConfigMap.put("contact", commonConfig.getContact() != null ? commonConfig.getContact() : "");
                    // iaaMode
                    iaaModeObj.put("enable", commonConfig.getIaaMode() != null ? commonConfig.getIaaMode() : false);
                    iaaModeObj.put("dialogStyle", commonConfig.getIaaDialogStyle() != null ? commonConfig.getIaaDialogStyle() : 2);
                    // hidePayEntry
                    platformCommonConfigMap.put("hidePayEntry", commonConfig.getHidePayEntry() != null ? commonConfig.getHidePayEntry() : false);
                    // imId 仅tt
                    if ("tt".equals(key)) platformCommonConfigMap.put("imId", commonConfig.getDouyinImId() != null ? commonConfig.getDouyinImId() : "");
                }
                
                // 更新现有配置
                fileContent = updateExistingCommonConfig(existingContent, platform, platformCommonConfigMap);
            } else {
                // 创建全新的配置文件内容
                java.util.LinkedHashMap<String, Object> commonConfigMap = new java.util.LinkedHashMap<>();
                String[] platforms = {"tt", "ks", "wx", "bd"};
                String platformKey = platformToKey(platform);
                
                // 只在对应平台生成详细配置，其他平台只保留空对象
                for (String pf : platforms) {
                    java.util.LinkedHashMap<String, Object> pfMap = new java.util.LinkedHashMap<>();
                    if (pf.equals(platformKey)) {
                        // 为当前平台生成详细配置
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

                        // 平台特殊字段
                        if ("tt".equals(pf)) {
                            pfMap.put("imId", "");
                        }
                    }
                    // 其他平台保留空对象
                    commonConfigMap.put(pf, pfMap);
                }
                
                // 只对当前platform赋值
                if (commonConfig != null && commonConfigMap.containsKey(platformKey)) {
                    java.util.Map<String, Object> pfMap = (java.util.Map<String, Object>) commonConfigMap.get(platformKey);
                    // homeCard.style
                    java.util.Map<String, Object> homeCard = (java.util.Map<String, Object>) pfMap.get("homeCard");
                    if (homeCard != null) {
                        homeCard.put("style", commonConfig.getHomeCardStyle() != null ? commonConfig.getHomeCardStyle() : 1);
                    }
                    // payCard.style
                    java.util.Map<String, Object> payCard = (java.util.Map<String, Object>) pfMap.get("payCard");
                    if (payCard != null) {
                        payCard.put("style", commonConfig.getPayCardStyle() != null ? commonConfig.getPayCardStyle() : 1);
                    }
                    // loginType
                    java.util.Map<String, Object> loginType = (java.util.Map<String, Object>) pfMap.get("loginType");
                    if (loginType != null) {
                        loginType.put("mine", commonConfig.getMineLoginType() != null ? commonConfig.getMineLoginType() : "anonymousLogin");
                        loginType.put("reader", commonConfig.getReaderLoginType() != null ? commonConfig.getReaderLoginType() : "anonymousLogin");
                    }
                    // contact
                    pfMap.put("contact", commonConfig.getContact() != null ? commonConfig.getContact() : "");
                    // iaaMode
                    java.util.LinkedHashMap<String, Object> iaaModeObj = new java.util.LinkedHashMap<>();
                    iaaModeObj.put("enable", commonConfig.getIaaMode() != null ? commonConfig.getIaaMode() : false);
                    iaaModeObj.put("dialogStyle", commonConfig.getIaaDialogStyle() != null ? commonConfig.getIaaDialogStyle() : 2);
                    pfMap.put("iaaMode", iaaModeObj);
                    pfMap.put("hidePayEntry", commonConfig.getHidePayEntry() != null ? commonConfig.getHidePayEntry() : false);
                    // imId 仅tt
                    if ("tt".equals(platformKey)) pfMap.put("imId", commonConfig.getDouyinImId() != null ? commonConfig.getDouyinImId() : "");
                }
                // 生成最终内容
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                StringBuilder finalSb = new StringBuilder();
                finalSb.append("export default ");
                String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(commonConfigMap);
                // 统一使用带单引号的外层键名并修复缩进
                jsonString = formatJsonString(jsonString);
                finalSb.append(jsonString);
                finalSb.append(";\n");
                fileContent = finalSb.toString();
            }
            
            java.nio.file.Files.write(configPath, fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-4-5] commonConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, fileContent, CreateNovelLogType.INFO);
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
    
    /**
     * 更新现有的commonConfig配置文件，只更新指定平台的配置，保留其他平台的配置
     * @param existingContent 现有的配置文件内容
     * @param platform 当前平台
     * @param platformCommonConfigMap 当前平台的通用配置内容
     * @return 更新后的完整配置文件内容
     */
    private String updateExistingCommonConfig(String existingContent, String platform, java.util.Map<String, Object> platformCommonConfigMap) {
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
            existingConfigMap.put(platformKey, platformCommonConfigMap);
            
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
            taskLogger.log(null, "解析现有commonConfig文件失败: " + e.getMessage(), CreateNovelLogType.ERROR);
            throw new RuntimeException("无法解析现有common配置文件内容: " + e.getMessage(), e);
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
     * 添加指定级别的缩进
     * @param sb StringBuilder
     * @param level 缩进级别
     */
    private void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }
    }
}
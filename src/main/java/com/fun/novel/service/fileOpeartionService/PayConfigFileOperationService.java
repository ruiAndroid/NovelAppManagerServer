package com.fun.novel.service.fileOpeartionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppLocalFileOperationService;
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
 * 支付配置文件操作服务
 * 处理payConfigs目录下的支付配置文件
 */
@Service
public class PayConfigFileOperationService extends AbstractConfigFileOperationService{
    private static final Logger log = LoggerFactory.getLogger(PayConfigFileOperationService.class);

    public void updatePayConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        //修改支付配置
        updatePayConfigFile( buildCode, platform, paymentConfig, rollbackActions);
    }

    public void createPayConfigLocalCodeFiles(String taskId,CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        //修改支付配置
        createPayConfigFile(taskId, buildCode, platform, paymentConfig, rollbackActions,true);
    }


    /**
     * 更新支付配置文件
     *
     */
    private void updatePayConfigFile(String buildCode, String platform,
                                     CreateNovelAppRequest.PaymentConfig payConfig,
                                     List<Runnable> rollbackActions){
        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "payConfigs";
        String configFile = configDir + File.separator + buildCode + ".js";
        Path configPath = Paths.get(configFile);
        String backupPath = configFile + ".bak";

        try {
            // 确保目录存在
            Files.createDirectories(Paths.get(configDir));
            // 如果文件不存在，直接返回，因为没有配置可以更新
            if (!Files.exists(configPath)) {
                return;
            }

            // 备份文件
            Files.copy(configPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(null, "回滚动作：还原payConfig.js", CreateNovelLogType.ERROR);
                    Files.copy(Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(Paths.get(backupPath));
                } catch (Exception ignore) {}
            });

            // 读取现有文件内容
            String existingContent = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);

            // 构造当前平台的支付配置
            java.util.LinkedHashMap<String, Object> platformPayConfigMap = new java.util.LinkedHashMap<>();

            // 根据平台类型构建配置结构
            String key = platformToKey(platform);
            switch (key) {
                case "tt":
                    platformPayConfigMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                    platformPayConfigMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                    platformPayConfigMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                    platformPayConfigMap.put("dou_zuan_pay", buildPayTypeMap(payConfig.getDouzuanPay()));
                    break;
                case "ks":
                    platformPayConfigMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                    platformPayConfigMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                    platformPayConfigMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                    break;
                case "wx":
                    platformPayConfigMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                    platformPayConfigMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                    platformPayConfigMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                    platformPayConfigMap.put("wx_virtual_pay", buildPayTypeMap(payConfig.getWxVirtualPay()));
                    break;
                case "bd":
                    platformPayConfigMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                    platformPayConfigMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                    break;
            }

            // 更新现有配置，只更新指定平台的配置
            String fileContent = updateExistingPayConfig(existingContent, platform, platformPayConfigMap);
            
            // 写入更新后的内容
            Files.write(configPath, fileContent.getBytes(StandardCharsets.UTF_8));

            // 操作成功后删除.bak
            try { Files.deleteIfExists(Paths.get(backupPath)); } catch (Exception ignore) {}
        } catch (Exception e) {
            // 还原自身
            try { Files.deleteIfExists(configPath); } catch (Exception ignore) {}
            throw new RuntimeException("payConfig配置文件更新失败: " + e.getMessage(), e);
        }
    }


    /**
     * 创建支付配置文件
     */
    private void createPayConfigFile(String taskId, String buildCode, String platform,
                                    CreateNovelAppRequest.PaymentConfig payConfig, 
                                    List<Runnable> rollbackActions,boolean withLogAndDelay) {
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
            
            String fileContent;
            // 如果文件已存在，读取并更新内容，否则创建新文件
            if (java.nio.file.Files.exists(configPath)) {
                // 读取现有文件内容
                String existingContent = new String(java.nio.file.Files.readAllBytes(configPath), java.nio.charset.StandardCharsets.UTF_8);
                
                // 构造当前平台的支付配置
                java.util.LinkedHashMap<String, Object> platformPayConfigMap = new java.util.LinkedHashMap<>();
                
                // 根据平台类型构建配置结构
                String key = platformToKey(platform);
                switch (key) {
                    case "tt":
                        platformPayConfigMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                        platformPayConfigMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                        platformPayConfigMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                        platformPayConfigMap.put("dou_zuan_pay", buildPayTypeMap(payConfig.getDouzuanPay()));
                        break;
                    case "ks":
                        platformPayConfigMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                        platformPayConfigMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                        platformPayConfigMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                        break;
                    case "wx":
                        platformPayConfigMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                        platformPayConfigMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                        platformPayConfigMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                        platformPayConfigMap.put("wx_virtual_pay", buildPayTypeMap(payConfig.getWxVirtualPay()));
                        break;
                    case "bd":
                        platformPayConfigMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                        platformPayConfigMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                        break;
                }
                
                // 更新现有配置
                fileContent = updateExistingPayConfig(existingContent, platform, platformPayConfigMap);
            } else {
                // 创建全新的配置文件内容
                java.util.LinkedHashMap<String, Object> payConfigMap = new java.util.LinkedHashMap<>();
                String platformKey = platformToKey(platform);
                
                // 只在对应平台生成详细配置，其他平台只保留空对象
                String[] allPlatforms = {"tt", "ks", "wx", "bd"};
                for (String pf : allPlatforms) {
                    java.util.LinkedHashMap<String, Object> pfMap = new java.util.LinkedHashMap<>();
                    if (pf.equals(platformKey)) {
                        // 为当前平台生成详细配置
                        switch (pf) {
                            case "tt":
                                pfMap.put("normal_pay", buildPayTypeMap(null));
                                pfMap.put("order_pay", buildPayTypeMap(null));
                                pfMap.put("renew_pay", buildPayTypeMap(null));
                                pfMap.put("dou_zuan_pay", buildPayTypeMap(null));
                                break;
                            case "ks":
                                pfMap.put("normal_pay", buildPayTypeMap(null));
                                pfMap.put("order_pay", buildPayTypeMap(null));
                                pfMap.put("renew_pay", buildPayTypeMap(null));
                                break;
                            case "wx":
                                pfMap.put("normal_pay", buildPayTypeMap(null));
                                pfMap.put("order_pay", buildPayTypeMap(null));
                                pfMap.put("renew_pay", buildPayTypeMap(null));
                                pfMap.put("wx_virtual_pay", buildPayTypeMap(null));
                                break;
                            case "bd":
                                pfMap.put("normal_pay", buildPayTypeMap(null));
                                pfMap.put("order_pay", buildPayTypeMap(null));
                                break;
                        }
                    }
                    // 其他平台保留空对象
                    payConfigMap.put(pf, pfMap);
                }
                
                // 只写入当前平台
                switch (platformKey) {
                    case "tt":
                        java.util.Map<String, Object> ttMap = (java.util.Map<String, Object>) payConfigMap.get("tt");
                        ttMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                        ttMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                        ttMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                        ttMap.put("dou_zuan_pay", buildPayTypeMap(payConfig.getDouzuanPay()));
                        break;
                    case "ks":
                        java.util.Map<String, Object> ksMap = (java.util.Map<String, Object>) payConfigMap.get("ks");
                        ksMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                        ksMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                        ksMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                        break;
                    case "wx":
                        java.util.Map<String, Object> wxMap = (java.util.Map<String, Object>) payConfigMap.get("wx");
                        wxMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                        wxMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                        wxMap.put("renew_pay", buildPayTypeMap(payConfig.getRenewPay()));
                        wxMap.put("wx_virtual_pay", buildPayTypeMap(payConfig.getWxVirtualPay()));
                        break;
                    case "bd":
                        java.util.Map<String, Object> bdMap = (java.util.Map<String, Object>) payConfigMap.get("bd");
                        bdMap.put("normal_pay", buildPayTypeMap(payConfig.getNormalPay()));
                        bdMap.put("order_pay", buildPayTypeMap(payConfig.getOrderPay()));
                        break;
                }
                
                // 生成最终内容
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                StringBuilder finalSb = new StringBuilder();
                finalSb.append("export default ");
                String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payConfigMap);
                // 统一使用带单引号的外层键名并修复缩进
                jsonString = formatJsonString(jsonString);
                finalSb.append(jsonString);
                finalSb.append(";\n");
                fileContent = finalSb.toString();
            }
            
            java.nio.file.Files.write(configPath, fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-4-4] payConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, fileContent, CreateNovelLogType.INFO);
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
    
    /**
     * 更新现有的payConfig配置文件，只更新指定平台的配置，保留其他平台的配置
     * @param existingContent 现有的配置文件内容
     * @param platform 当前平台
     * @param platformPayConfigMap 当前平台的支付配置内容
     * @return 更新后的完整配置文件内容
     */
    private String updateExistingPayConfig(String existingContent, String platform, java.util.Map<String, Object> platformPayConfigMap) {
        // 将平台名称映射到配置文件中的键名
        String platformKey = platformToKey(platform);
        
        try {
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
            existingConfigMap.put(platformKey, platformPayConfigMap);
            
            // 重新生成配置文件内容
            StringBuilder finalSb = new StringBuilder();
            finalSb.append("export default ");
            String jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(existingConfigMap);
            finalSb.append(formatJsonString(jsonStr));
            finalSb.append(";\n");
            
            return finalSb.toString();
        } catch (Exception e) {
            taskLogger.log(null, "解析现有payConfig文件失败: " + e.getMessage(), CreateNovelLogType.ERROR);
            throw new RuntimeException("无法解析现有pay配置文件内容: " + e.getMessage(), e);
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

    public void deletePayConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions, boolean isLast) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "payConfigs";
        String configFile = configDir + File.separator + buildCode + ".js";
        Path configPath = Paths.get(configFile);
        String backupPath = configFile + ".bak";
        
        try {
            // 检查文件是否存在
            if (!Files.exists(configPath)) {
                log.warn("payConfig文件不存在: {}", configFile);
                return;
            }

            if (isLast) {
                // 如果是最后一个平台，则直接删除整个文件
                Files.copy(configPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // 添加回滚动作
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(null, "回滚动作：还原payConfig文件", CreateNovelLogType.ERROR);
                        Files.copy(Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
                
                // 删除文件
                Files.deleteIfExists(configPath);
                
                // 操作成功后删除备份文件
                Files.deleteIfExists(Paths.get(backupPath));
                
                log.info("成功删除payConfig文件: {}", configFile);
            } else {
                // 如果不是最后一个平台，则只删除对应平台的配置块
                deletePlatformPayConfig(configPath, platform, rollbackActions);
            }
        } catch (Exception e) {
            log.error("删除payConfig文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除payConfig文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除指定平台的支付配置块
     * @param configPath 配置文件路径
     * @param platform 平台
     * @param rollbackActions 回滚动作列表
     */
    private void deletePlatformPayConfig(Path configPath, String platform, List<Runnable> rollbackActions) {
        try {
            // 读取原内容
            String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
            
            // 备份原文件
            String backupPath = configPath.toString() + ".bak";
            Files.copy(configPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // 添加回滚动作
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(null, "回滚动作：还原payConfig文件", CreateNovelLogType.ERROR);
                    Files.copy(Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(Paths.get(backupPath));
                } catch (Exception ignore) {}
            });
            
            // 将平台名称映射到配置文件中的键名
            String platformKey = platformToKey(platform);
            
            // 使用Jackson解析JSON配置
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置ObjectMapper以允许单引号
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            // 配置ObjectMapper以允许不带引号的字段名
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            
            String jsonPart = content.substring(content.indexOf("export default ") + "export default ".length());
            if (jsonPart.endsWith(";")) {
                jsonPart = jsonPart.substring(0, jsonPart.length() - 1);
            }
            
            // 解析现有配置
            java.util.Map<String, Object> existingConfigMap = objectMapper.readValue(jsonPart, java.util.Map.class);
            
            // 清空指定平台的配置而不是删除整个平台配置块
            existingConfigMap.put(platformKey, new java.util.LinkedHashMap<String, Object>());
            
            // 重新生成配置文件内容
            StringBuilder finalSb = new StringBuilder();
            finalSb.append("export default ");
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(existingConfigMap);
            // 统一使用带单引号的外层键名并修复缩进
            jsonString = formatJsonString(jsonString);
            finalSb.append(jsonString);
            finalSb.append(";\n");
            
            // 写入新内容
            Files.write(configPath, finalSb.toString().getBytes(StandardCharsets.UTF_8));
            
            // 操作成功后删除备份文件
            Files.deleteIfExists(Paths.get(backupPath));
            
            log.info("成功清空payConfig文件中平台 {} 的配置: {}", platformKey, configPath.toString());
        } catch (Exception e) {
            log.error("清空payConfig文件中平台配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("清空payConfig文件中平台配置失败: " + e.getMessage(), e);
        }
    }
}
package com.fun.novel.service.fileOpeartionService;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 支付配置文件操作服务
 * 处理payConfigs目录下的支付配置文件
 */
@Service
public class PayConfigFileOperationService extends AbstractConfigFileOperationService{

    public void updatePayConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        //修改支付配置
        createPayConfigFile(null, buildCode, platform, paymentConfig, rollbackActions, false);
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
     * 处理支付配置文件
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
            existingConfigMap.put(platformKey, platformPayConfigMap);
            
            // 重新生成配置文件内容
            StringBuilder finalSb = new StringBuilder();
            finalSb.append("export default ");
            finalSb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(existingConfigMap));
            finalSb.append(";\n");
            
            return finalSb.toString();
        } catch (Exception e) {
            // 如果解析失败，使用后备方案（创建全新的配置文件）
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
            
            // 更新当前平台配置
            String key = platformToKey(platform);
            if (payConfigMap.containsKey(key)) {
                payConfigMap.put(key, platformPayConfigMap);
            }
            
            // 生成最终内容
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            try {
                StringBuilder finalSb = new StringBuilder();
                finalSb.append("export default ");
                finalSb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payConfigMap));
                finalSb.append(";\n");
                return finalSb.toString();
            } catch (Exception ex) {
                throw new RuntimeException("无法生成pay配置文件内容", ex);
            }
        }
    }
}
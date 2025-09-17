package com.fun.novel.service.fileOpeartionService;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.dto.CreateNovelLogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * UI配置文件操作服务
 * 处理UI配置相关的本地代码文件操作
 */
@Service
public class UiConfigFileOperationService extends AbstractConfigFileOperationService {

    private static final Logger log = LoggerFactory.getLogger(UiConfigFileOperationService.class);

    /**
     * 创建UI配置相关的本地代码文件
     *
     * @param taskId          任务ID
     * @param params          创建应用请求参数
     * @param rollbackActions 回滚操作列表
     */
    public void createUiConfigLocalCodeFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.PaymentConfig paymentConfig = params.getPaymentConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        createUiConfigFile(taskId, buildCode,platform, params.getUiConfig(), rollbackActions, true);
    }

    /**
     * 更新UI配置相关的本地代码文件
     *
     * @param params          创建应用请求参数
     * @param rollbackActions 回滚操作列表
     */
    public void updateUiConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        String buildCode = commonConfig.getBuildCode();

    }


    private void createUiConfigFile(String taskId, String buildCode,String platform, CreateNovelAppRequest.UiConfig uiConfig, List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-6] 开始处理uiConfig配置文件: " + buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "uiConfigs" + File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
            try {
                Thread.sleep(FILE_STEP_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "uiConfigs";
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
                        taskLogger.log(taskId, "回滚动作：还原uiConfig.js", CreateNovelLogType.ERROR);
                        java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                    } catch (Exception ignore) {
                    }
                });
            } else {
                // 文件不存在，回滚时删除
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(taskId, "回滚动作：删除uiConfig.js", CreateNovelLogType.ERROR);
                        java.nio.file.Files.deleteIfExists(configPath);
                    } catch (Exception ignore) {
                    }
                });
            }
            String fileContent;
            // 如果文件已存在，读取并更新内容，否则创建新文件
            if (java.nio.file.Files.exists(configPath)) {
                // 读取现有文件内容
                String existingContent = new String(java.nio.file.Files.readAllBytes(configPath), java.nio.charset.StandardCharsets.UTF_8);
                // 构造当前平台的ui配置
                java.util.LinkedHashMap<String, Object> platformUiConfigMap = new java.util.LinkedHashMap<>();
                platformUiConfigMap.put("homeCardStyle", 1);
                platformUiConfigMap.put("payCardStyle", 1);

                // 只对当前platform赋值
                String key = platformToKey(platform);
                if (uiConfig != null) {
                    platformUiConfigMap.put("homeCardStyle", uiConfig.getHomeCardStyle() != null ? uiConfig.getHomeCardStyle() : 1);
                    platformUiConfigMap.put("payCardStyle", uiConfig.getPayCardStyle() != null ? uiConfig.getPayCardStyle() : 1);
                }
                // 更新现有配置
                fileContent = updateExistingUiConfig(existingContent, platform, platformUiConfigMap);
            }else{
                // 创建全新的配置文件内容
                java.util.LinkedHashMap<String, Object> uiConfigMap = new java.util.LinkedHashMap<>();
                String[] platforms = {"tt", "ks", "wx", "bd"};
                String platformKey = platformToKey(platform);
                // 只在对应平台生成详细配置，其他平台只保留空对象
                for (String pf : platforms) {
                    java.util.LinkedHashMap<String, Object> pfMap = new java.util.LinkedHashMap<>();
                    if (pf.equals(platformKey)) {
                        pfMap.put("homeCardStyle", 1);
                        pfMap.put("payCardStyle", 1);
                    }
                    // 其他平台保留空对象
                    uiConfigMap.put(pf, pfMap);
                }

                // 只对当前platform赋值
                if (uiConfig != null && uiConfigMap.containsKey(platformKey)) {
                    java.util.Map<String, Object> pfMap = (java.util.Map<String, Object>) uiConfigMap.get(platformKey);
                    pfMap.put("homeCardStyle", uiConfig.getHomeCardStyle() != null ? uiConfig.getHomeCardStyle() : 1);
                    pfMap.put("payCardStyle", uiConfig.getPayCardStyle() != null ? uiConfig.getPayCardStyle() : 1);
                }

                // 生成最终内容
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                StringBuilder finalSb = new StringBuilder();
                finalSb.append("export default ");
                String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(uiConfigMap);
                // 统一使用带单引号的外层键名并修复缩进
                jsonString = formatJsonString(jsonString);
                finalSb.append(jsonString);
                finalSb.append(";\n");
                fileContent = finalSb.toString();
            }

            java.nio.file.Files.write(configPath, fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-6] commonConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, fileContent, CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}





        } catch (Exception e) {
            // 还原自身
            try {
                java.nio.file.Files.deleteIfExists(configPath);
            } catch (Exception ignore) {
            }
            throw new RuntimeException("uiConfig配置文件处理失败: " + e.getMessage(), e);
        }

    }

    /**
     * 更新现有的uiConfig配置文件，只更新指定平台的配置，保留其他平台的配置
     * @param existingContent 现有的配置文件内容
     * @param platform 当前平台
     * @param platformUiConfigMap 当前平台的ui配置内容
     * @return 更新后的完整配置文件内容
     */
    private String updateExistingUiConfig(String existingContent, String platform, java.util.Map<String, Object> platformUiConfigMap) {
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
            existingConfigMap.put(platformKey, platformUiConfigMap);

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
            taskLogger.log(null, "解析现有uiConfig文件失败: " + e.getMessage(), CreateNovelLogType.ERROR);
            throw new RuntimeException("无法解析现有ui配置文件内容: " + e.getMessage(), e);
        }
    }



}
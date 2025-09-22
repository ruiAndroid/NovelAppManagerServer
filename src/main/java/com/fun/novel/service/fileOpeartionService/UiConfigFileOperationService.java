package com.fun.novel.service.fileOpeartionService;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        CreateNovelAppRequest.UiConfig uiConfig = params.getUiConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        createThemeFile(taskId, buildCode, uiConfig, rollbackActions, true);
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

    public void deleteUiConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions,boolean isLast) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        deleteThemeFile(buildCode, rollbackActions, isLast);
        deleteUiConfigFile(buildCode, platform, rollbackActions, isLast);

    }
    /**
     * 删除指定appId的uiConfig配置
     *
     */
    public void deleteUiConfigFile(String buildCode, String platform, List<Runnable> rollbackActions, boolean isLast) {

        String configDir = buildWorkPath + File.separator + "src" + File.separator + "modules" + File.separator + "mod_config" + File.separator + "uiConfigs";
        String configFile = configDir + File.separator + buildCode + ".js";
        Path configPath = Paths.get(configFile);
        String backupPath = configFile + ".bak";

        try {
            // 检查文件是否存在
            if (!Files.exists(configPath)) {
                log.warn("uiConfig文件不存在: {}", configFile);
                return;
            }

            if (isLast) {
                // 如果是最后一个平台，则直接删除整个文件
                Files.copy(configPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // 添加回滚动作
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(null, "回滚动作：还原uiConfig文件", CreateNovelLogType.ERROR);
                        Files.copy(Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });

                // 删除文件
                Files.deleteIfExists(configPath);

                // 操作成功后删除备份文件
                Files.deleteIfExists(Paths.get(backupPath));

                log.info("成功删除uiConfig文件: {}", configFile);
            } else {
                // 如果不是最后一个平台，则只删除对应平台的配置块
                deletePlatformUiConfig(configPath, platform, rollbackActions);
            }
        } catch (Exception e) {
            log.error("删除uiConfig文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除uiConfig文件失败: " + e.getMessage(), e);
        }

    }

    /**
     * 处理主题文件
     */
    private void createThemeFile(String taskId, String buildCode, CreateNovelAppRequest.UiConfig uiConfig,
                                 List<Runnable> rollbackActions, boolean withLogAndDelay) {
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-5] 开始处理主题文件: " + buildWorkPath + File.separator + "src" + File.separator + "common" + File.separator + "styles" + File.separator + "theme.less", CreateNovelLogType.PROCESSING);
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
            taskLogger.log(taskId, "[2-5] 备份主题文件完成", CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 读取原内容
            java.util.List<String> lines = java.nio.file.Files.readAllLines(themePath, java.nio.charset.StandardCharsets.UTF_8);
            // 构造新主题色变量
            String mainTheme = uiConfig.getMainTheme();
            String secondTheme = uiConfig.getSecondTheme();
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
                taskLogger.log(taskId, "[2-5] 主题色变量已存在，跳过处理", CreateNovelLogType.SUCCESS);
                // 操作成功后删除theme.bak
                try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); } catch (Exception ignore) {}
                return;
            }

            if (!foundPrimary) lines.add(line1);
            if (!foundSecond) lines.add(line2);
            // 写回文件
            java.nio.file.Files.write(themePath, lines, java.nio.charset.StandardCharsets.UTF_8);
            taskLogger.log(taskId, "[2-5] 新增主题色变量完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, "[2-5] 新增内容：\n" + line1 + "\n" + line2, CreateNovelLogType.INFO);
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



    //删除主题文件
    private void deleteThemeFile(String buildCode, List<Runnable> rollbackActions, boolean isLast) {
        if(!isLast){
            log.warn("删除主题文件，当前还存在其他平台小程序，不需要删除" );
            return;
        }
        String themeConfigDir = buildWorkPath + File.separator + "src" + File.separator + "common" + File.separator + "styles";
        String themeConfigFile = themeConfigDir + File.separator + "theme.less";
        java.nio.file.Path themeConfigPath = java.nio.file.Paths.get(themeConfigFile);

        try {
            // 检查文件是否存在
            if (!java.nio.file.Files.exists(themeConfigPath)) {
                log.warn("主题文件不存在: {}", themeConfigFile);
                return;
            }

            // 读取原内容
            java.util.List<String> lines = java.nio.file.Files.readAllLines(themeConfigPath, java.nio.charset.StandardCharsets.UTF_8);

            // 备份原文件
            String backupPath = themeConfigFile + ".bak";
            java.nio.file.Files.copy(themeConfigPath, java.nio.file.Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // 添加回滚动作
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(null, "回滚动作：还原主题文件", CreateNovelLogType.ERROR);
                    java.nio.file.Files.copy(java.nio.file.Paths.get(backupPath), themeConfigPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));
                } catch (Exception ignore) {}
            });

            // 构造要删除的行
            String primaryColorLine = "@primary-color-" + buildCode + ":";
            String secondColorLine = "@second-color-" + buildCode + ":";

            // 过滤掉包含构建代码的行
            java.util.List<String> newLines = new java.util.ArrayList<>();
            for (String line : lines) {
                String trimmedLine = line.trim();
                // 如果行不包含当前构建代码，则保留
                if (!trimmedLine.startsWith(primaryColorLine) && !trimmedLine.startsWith(secondColorLine)) {
                    newLines.add(line);
                }
            }

            // 写入新内容
            java.nio.file.Files.write(themeConfigPath, newLines, java.nio.charset.StandardCharsets.UTF_8);

            // 操作成功后删除备份文件
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(backupPath));

            log.info("成功删除主题文件中的构建代码配置: {}", buildCode);
        } catch (Exception e) {
            log.error("删除主题文件配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除主题文件配置失败: " + e.getMessage(), e);
        }
    }


    /**
     * 删除指定平台的ui配置块
     * @param configPath 配置文件路径
     * @param platform 平台
     * @param rollbackActions 回滚动作列表
     */
    private void deletePlatformUiConfig(Path configPath, String platform, List<Runnable> rollbackActions) {
        try {
            // 读取原内容
            String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);

            // 备份原文件
            String backupPath = configPath.toString() + ".bak";
            Files.copy(configPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // 添加回滚动作
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(null, "回滚动作：还原uiConfig文件", CreateNovelLogType.ERROR);
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

            log.info("成功清空uiConfig文件中平台 {} 的配置: {}", platformKey, configPath.toString());
        } catch (Exception e) {
            log.error("清空uiConfig文件中平台配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("清空uiConfig文件中平台配置失败: " + e.getMessage(), e);
        }
    }
}
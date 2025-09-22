package com.fun.novel.service.fileOpeartionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.dto.CreateNovelLogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 广告配置文件操作服务
 * 处理adConfigs目录下的广告配置文件
 */
@Service
public class AdConfigFileOperationService extends AbstractConfigFileOperationService {
    
    private static final Logger log = LoggerFactory.getLogger(AdConfigFileOperationService.class);

    public void createAdConfigLocalCodeFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.AdConfig adConfig = params.getAdConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        createAdConfigFile(taskId, buildCode, platform, adConfig, rollbackActions, true);

    }

    public void updateAdConfigLocalCodeFiles( CreateNovelAppRequest params, List<Runnable> rollbackActions){
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.AdConfig adConfig = params.getAdConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();
        createAdConfigFile(null, buildCode, platform, adConfig, rollbackActions, false);
    }


    private void createAdConfigFile(String taskId, String buildCode, String platform, CreateNovelAppRequest.AdConfig adConfig, List<Runnable> rollbackActions, boolean withLogAndDelay){
        if (withLogAndDelay) {
            taskLogger.log(taskId, "[2-3-1] 开始处理adConfig配置文件: " + buildWorkPath + java.io.File.separator + "src" + java.io.File.separator + "modules" + java.io.File.separator + "mod_config" + java.io.File.separator + "adConfigs" + java.io.File.separator + buildCode + ".js", CreateNovelLogType.PROCESSING);
            try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        String configDir = buildWorkPath + java.io.File.separator + "src" + java.io.File.separator + "modules" + java.io.File.separator + "mod_config" + java.io.File.separator + "adConfigs";
        String configFile = configDir + java.io.File.separator + buildCode + ".js";
        Path configPath = Paths.get(configFile);
        String backupPath = configFile + ".bak";
        try {
            // 确保目录存在
            Files.createDirectories(Paths.get(configDir));
            // 如果文件已存在，先备份
            backupFileAndAddRollback(taskId, configPath, rollbackActions);

            String fileContent;
            // 如果文件已存在，读取并更新内容，否则创建新文件
            if (Files.exists(configPath)) {
                // 读取现有文件内容
                String existingContent = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
                
                // 构造当前平台的广告配置
                java.util.LinkedHashMap<String, Object> platformAdConfigMap = new java.util.LinkedHashMap<>();
                
                // 默认结构
                platformAdConfigMap.put("rewardAd", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                platformAdConfigMap.put("banner", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                platformAdConfigMap.put("interstitial", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                platformAdConfigMap.put("feed", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                
                // 写入对应平台的广告配置
                if (adConfig != null) {
                    // rewardAd
                    if (adConfig.getRewardAd() != null) {
                        java.util.Map<String, Object> rewardAd = new java.util.LinkedHashMap<>();
                        boolean enable = Boolean.TRUE.equals(adConfig.getRewardAd().getEnabled());
                        rewardAd.put("enable", enable);
                        if (enable) {
                            rewardAd.put("id", adConfig.getRewardAd().getRewardAdId());
                            rewardAd.put("count", adConfig.getRewardAd().getRewardCount());
                        }
                        platformAdConfigMap.put("rewardAd", rewardAd);
                    }
                    // interstitialAd
                    if (adConfig.getInterstitialAd() != null) {
                        java.util.Map<String, Object> interstitialAd = new java.util.LinkedHashMap<>();
                        boolean enable = Boolean.TRUE.equals(adConfig.getInterstitialAd().getEnabled());
                        interstitialAd.put("enable", enable);
                        if (enable) {
                            Object idVal = adConfig.getInterstitialAd().getInterstitialAdId();
                            if ("kuaishou".equals(platform) && idVal != null) {
                                try {
                                    interstitialAd.put("id", Long.parseLong(idVal.toString()));
                                } catch (Exception ignore) { interstitialAd.put("id", idVal); }
                            } else {
                                interstitialAd.put("id", idVal);
                            }
                            interstitialAd.put("count", adConfig.getInterstitialAd().getInterstitialCount());
                        }
                        platformAdConfigMap.put("interstitial", interstitialAd);
                    }
                    // bannerAd
                    if (adConfig.getBannerAd() != null) {
                        java.util.Map<String, Object> bannerAd = new java.util.LinkedHashMap<>();
                        boolean enable = Boolean.TRUE.equals(adConfig.getBannerAd().getEnabled());
                        bannerAd.put("enable", enable);
                        if (enable) {
                            bannerAd.put("id", adConfig.getBannerAd().getBannerAdId());
                            bannerAd.put("type", 100033797);
                        }
                        platformAdConfigMap.put("banner", bannerAd);
                    }
                    //feedAd
                    if (adConfig.getFeedAd() != null) {
                        java.util.Map<String, Object> feedAd = new java.util.LinkedHashMap<>();
                        boolean enable = Boolean.TRUE.equals(adConfig.getFeedAd().getEnabled());
                        feedAd.put("enable", enable);
                        if (enable) {
                            feedAd.put("id", adConfig.getFeedAd().getFeedAdId());
                            feedAd.put("type", 100011054);
                        }
                        platformAdConfigMap.put("feed", feedAd);
                    }
                }
                
                // 更新现有配置
                fileContent = updateExistingAdConfig(existingContent, platform, platformAdConfigMap);
            } else {
                // 创建全新的配置文件内容
                // 解析为对象再写入广告配置
                ObjectMapper objectMapper = new ObjectMapper();
                // 先构造一个Map结构
                java.util.LinkedHashMap<String, Object> adConfigMap = new java.util.LinkedHashMap<>();
                String[] allPlatforms = {"tt", "ks", "wx", "bd"};
                String platformKey = platformToKey(platform);
                
                // 只在对应平台生成详细配置，其他平台只保留空对象
                for (String pf : allPlatforms) {
                    java.util.LinkedHashMap<String, Object> pfMap = new java.util.LinkedHashMap<>();
                    if (pf.equals(platformKey)) {
                        // 为当前平台生成详细配置
                        pfMap.put("rewardAd", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                        pfMap.put("banner", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                        pfMap.put("interstitial", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                        pfMap.put("feed", new java.util.LinkedHashMap<String, Object>() {{ put("enable", false); }});
                    }
                    // 其他平台保留空对象
                    adConfigMap.put(pf, pfMap);
                }
                
                // 写入对应平台的广告配置
                if (adConfig != null) {
                    // rewardAd
                    if (adConfig.getRewardAd() != null) {
                        java.util.Map<String, Object> rewardAd = new java.util.LinkedHashMap<>();
                        boolean enable = Boolean.TRUE.equals(adConfig.getRewardAd().getEnabled());
                        rewardAd.put("enable", enable);
                        if (enable) {
                            rewardAd.put("id", adConfig.getRewardAd().getRewardAdId());
                            rewardAd.put("count", adConfig.getRewardAd().getRewardCount());
                        }
                        ((java.util.Map<String, Object>)adConfigMap.get(platformToKey(platform))).put("rewardAd", rewardAd);
                    }
                    // interstitialAd
                    if (adConfig.getInterstitialAd() != null) {
                        java.util.Map<String, Object> interstitialAd = new java.util.LinkedHashMap<>();
                        boolean enable = Boolean.TRUE.equals(adConfig.getInterstitialAd().getEnabled());
                        interstitialAd.put("enable", enable);
                        if (enable) {
                            Object idVal = adConfig.getInterstitialAd().getInterstitialAdId();
                            if ("kuaishou".equals(platform) && idVal != null) {
                                try {
                                    interstitialAd.put("id", Long.parseLong(idVal.toString()));
                                } catch (Exception ignore) { interstitialAd.put("id", idVal); }
                            } else {
                                interstitialAd.put("id", idVal);
                            }
                            interstitialAd.put("count", adConfig.getInterstitialAd().getInterstitialCount());
                        }
                        ((java.util.Map<String, Object>)adConfigMap.get(platformToKey(platform))).put("interstitial", interstitialAd);
                    }
                    // bannerAd
                    if (adConfig.getBannerAd() != null) {
                        java.util.Map<String, Object> bannerAd = new java.util.LinkedHashMap<>();
                        boolean enable = Boolean.TRUE.equals(adConfig.getBannerAd().getEnabled());
                        bannerAd.put("enable", enable);
                        if (enable) {
                            bannerAd.put("id", adConfig.getBannerAd().getBannerAdId());
                            bannerAd.put("type", 100033797);
                        }
                        ((java.util.Map<String, Object>)adConfigMap.get(platformToKey(platform))).put("banner", bannerAd);
                    }
                    //feedAd
                    if (adConfig.getFeedAd() != null) {
                        java.util.Map<String, Object> feedAd = new java.util.LinkedHashMap<>();
                        boolean enable = Boolean.TRUE.equals(adConfig.getFeedAd().getEnabled());
                        feedAd.put("enable", enable);
                        if (enable) {
                            feedAd.put("id", adConfig.getFeedAd().getFeedAdId());
                            feedAd.put("type", 100011054);
                        }
                        ((java.util.Map<String, Object>)adConfigMap.get(platformToKey(platform))).put("feed", feedAd);
                    }
                }
                
                // 生成最终内容
                StringBuilder finalSb = new StringBuilder();
                finalSb.append("export default ");
                String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adConfigMap);
                // 统一使用带单引号的外层键名并修复缩进
                jsonString = formatJsonString(jsonString);
                finalSb.append(jsonString);
                finalSb.append(";\n");
                fileContent = finalSb.toString();
            }

            Files.write(configPath, fileContent.getBytes(StandardCharsets.UTF_8));
            taskLogger.log(taskId, "[2-3-1] adConfig配置文件写入完成", CreateNovelLogType.SUCCESS);
            taskLogger.log(taskId, fileContent, CreateNovelLogType.INFO);
            if (withLogAndDelay) {
                try { Thread.sleep(FILE_STEP_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            // 操作成功后删除.bak
            deleteBackupFile(backupPath);
        } catch (Exception e) {
            // 还原自身
            try { Files.deleteIfExists(configPath); } catch (Exception ignore) {} 
            throw new RuntimeException("adConfig配置文件处理失败: " + e.getMessage(), e);
        }
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

    /**
     * 更新现有的adConfig配置文件，只更新指定平台的配置，保留其他平台的配置
     * @param existingContent 现有的配置文件内容
     * @param platform 当前平台
     * @param platformAdConfigMap 当前平台的广告配置内容
     * @return 更新后的完整配置文件内容
     */
    private String updateExistingAdConfig(String existingContent, String platform, java.util.Map<String, Object> platformAdConfigMap) {
        try {
            // 将平台名称映射到配置文件中的键名
            String platformKey = platformToKey(platform);

            // 使用正则表达式提取JSON部分
            String jsonPart = extractJsonFromJsFile(existingContent);
            
            // 解析现有配置
            ObjectMapper objectMapper = new ObjectMapper();
            java.util.Map<String, Object> existingConfigMap = objectMapper.readValue(jsonPart, java.util.Map.class);

            // 更新指定平台的配置
            existingConfigMap.put(platformKey, platformAdConfigMap);

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
            // 如果解析失败，尝试使用正则表达式方式更新配置
            try {
                return updateConfigWithRegex(existingContent, platform, platformAdConfigMap);
            } catch (Exception ex) {
                // 如果正则表达式方式也失败了，使用更健壮的方法
                try {
                    return updateConfigManually(existingContent, platform, platformAdConfigMap);
                } catch (Exception exc) {
                    throw new RuntimeException("无法更新ad配置文件内容: " + exc.getMessage(), exc);
                }
            }
        }
    }

    /**
     * 从JS文件中提取JSON部分
     * @param jsContent JS文件内容
     * @return JSON字符串
     */
    private String extractJsonFromJsFile(String jsContent) {
        String jsonPart = jsContent.substring(jsContent.indexOf("export default ") + "export default ".length());
        if (jsonPart.endsWith(";")) {
            jsonPart = jsonPart.substring(0, jsonPart.length() - 1);
        }
        return jsonPart.trim();
    }

    /**
     * 使用正则表达式方式更新配置（后备方案）
     * @param existingContent 现有配置内容
     * @param platform 平台
     * @param platformAdConfigMap 平台广告配置
     * @return 更新后的配置内容
     */
    private String updateConfigWithRegex(String existingContent, String platform, java.util.Map<String, Object> platformAdConfigMap) {
        try {
            // 将平台名称映射到配置文件中的键名
            String platformKey = platformToKey(platform);
            
            // 构造新的平台配置JSON字符串
            ObjectMapper objectMapper = new ObjectMapper();
            String newPlatformConfigJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(platformAdConfigMap);
            // 统一使用不带引号的内层键名并修复缩进
            newPlatformConfigJson = formatJsonString(newPlatformConfigJson);
            
            // 构造替换后的平台配置内容，外层键使用单引号
            StringBuilder newPlatformConfigBuilder = new StringBuilder();
            newPlatformConfigBuilder.append("'").append(platformKey).append("': ");
            newPlatformConfigBuilder.append(newPlatformConfigJson);
            
            // 查找现有平台配置块（支持带引号和不带引号两种格式）
            String platformPattern = "['\"]?" + platformKey + "['\"]?\\s*:\\s*\\{";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(platformPattern);
            java.util.regex.Matcher matcher = pattern.matcher(existingContent);
            
            if (matcher.find()) {
                // 如果找到匹配项，查找配置块的开始和结束位置
                int startMatchPos = matcher.start();
                int openBracePos = existingContent.indexOf("{", matcher.end() - 1);
                
                if (openBracePos != -1) {
                    // 查找匹配的结束大括号
                    int closeBracePos = findMatchingBrace(existingContent, openBracePos);
                    if (closeBracePos != -1) {
                        // 替换平台配置块
                        StringBuilder result = new StringBuilder();
                        result.append(existingContent.substring(0, startMatchPos));
                        result.append(newPlatformConfigBuilder.toString());
                        result.append(existingContent.substring(closeBracePos + 1));
                        
                        // 确保结尾格式正确
                        String resultStr = result.toString();
                        if (!resultStr.endsWith(";\n")) {
                            if (resultStr.endsWith(";")) {
                                resultStr = resultStr.substring(0, resultStr.length() - 1) + "\n;";
                            } else if (resultStr.endsWith("\n")) {
                                resultStr = resultStr.substring(0, resultStr.length() - 1) + ";\n";
                            } else {
                                resultStr += ";\n";
                            }
                        }
                        return resultStr;
                    }
                }
            } else {
                // 如果没有找到匹配项，则添加新的平台配置
                // 查找配置对象的结束位置
                int endIndex = existingContent.lastIndexOf("}");
                if (endIndex > 0) {
                    StringBuilder sb = new StringBuilder(existingContent);
                    // 检查是否需要添加逗号
                    if (sb.charAt(endIndex - 1) != '{' && sb.charAt(endIndex - 1) != ',') {
                        sb.insert(endIndex, ",");
                    }
                    // 添加新的平台配置，外层键使用单引号
                    sb.insert(endIndex, "\n    " + newPlatformConfigBuilder.toString() + "\n");
                    return sb.toString();
                }
            }
            
            // 如果无法添加新平台，返回原始内容
            return existingContent;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update config with regex", e);
        }
    }
    
    /**
     * 手动更新配置（最终后备方案）
     * @param existingContent 现有配置内容
     * @param platform 平台
     * @param platformAdConfigMap 平台广告配置
     * @return 更新后的配置内容
     */
    private String updateConfigManually(String existingContent, String platform, java.util.Map<String, Object> platformAdConfigMap) {
        // 将平台名称映射到配置文件中的键名
        String platformKey = platformToKey(platform);
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String newPlatformConfigJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(platformAdConfigMap);
            // 统一使用不带引号的内层键名并修复缩进
            newPlatformConfigJson = formatJsonString(newPlatformConfigJson);
            
            // 查找平台配置的位置（支持带引号和不带引号两种格式）
            String platformKeyPattern = "['\"]?" + platformKey + "['\"]?";
            int platformStartIndex = existingContent.indexOf(platformKeyPattern);
            
            if (platformStartIndex != -1) {
                // 找到平台配置，替换它
                int colonIndex = existingContent.indexOf(":", platformStartIndex);
                if (colonIndex != -1) {
                    // 找到开始大括号
                    int startBraceIndex = existingContent.indexOf("{", colonIndex);
                    if (startBraceIndex != -1) {
                        // 找到匹配的结束大括号
                        int endBraceIndex = findMatchingBrace(existingContent, startBraceIndex);
                        if (endBraceIndex != -1) {
                            // 构造新的平台配置，外层键使用单引号
                            StringBuilder newPlatformConfigBuilder = new StringBuilder();
                            newPlatformConfigBuilder.append("'").append(platformKey).append("': ");
                            newPlatformConfigBuilder.append(newPlatformConfigJson);
                            
                            // 替换平台配置
                            StringBuilder sb = new StringBuilder(existingContent);
                            sb.replace(platformStartIndex, endBraceIndex + 1, newPlatformConfigBuilder.toString());
                            return sb.toString();
                        }
                    }
                }
            } else {
                // 没有找到平台配置，添加新的
                int endIndex = existingContent.lastIndexOf("}");
                if (endIndex > 0) {
                    StringBuilder sb = new StringBuilder(existingContent);
                    // 检查是否需要添加逗号
                    if (sb.charAt(endIndex - 1) != '{' && sb.charAt(endIndex - 1) != ',') {
                        sb.insert(endIndex, ",");
                    }
                    // 构造新的平台配置，外层键使用单引号
                    StringBuilder newPlatformConfigBuilder = new StringBuilder();
                    newPlatformConfigBuilder.append("\n    '").append(platformKey).append("': ");
                    newPlatformConfigBuilder.append(newPlatformConfigJson);
                    sb.insert(endIndex, newPlatformConfigBuilder.toString());
                    return sb.toString();
                }
            }
            
            // 如果所有方法都失败了，返回原始内容
            return existingContent;
        } catch (Exception e) {
            throw new RuntimeException("Manual config update failed", e);
        }
    }

    /**
     * 查找匹配的大括号
     * @param content 内容
     * @param startIndex 开始索引
     * @return 匹配的大括号索引，如果未找到返回-1
     */
    private int findMatchingBrace(String content, int startIndex) {
        int braceCount = 1;
        for (int i = startIndex + 1; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void deleteAdConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions, boolean isLast) {
        CreateNovelAppRequest.CommonConfig commonConfig = params.getCommonConfig();
        CreateNovelAppRequest.BaseConfig baseConfig = params.getBaseConfig();
        CreateNovelAppRequest.AdConfig adConfig = params.getAdConfig();

        String buildCode = commonConfig.getBuildCode();
        String platform = baseConfig.getPlatform();

        String configDir = buildWorkPath + java.io.File.separator + "src" + java.io.File.separator + "modules" + java.io.File.separator + "mod_config" + java.io.File.separator + "adConfigs";
        String configFile = configDir + java.io.File.separator + buildCode + ".js";
        Path configPath = Paths.get(configFile);
        String backupPath = configFile + ".bak";
        
        try {
            // 检查文件是否存在
            if (!Files.exists(configPath)) {
                log.warn("adConfig文件不存在: {}", configFile);
                return;
            }

            if (isLast) {
                // 如果是最后一个平台，则直接删除整个文件
                Files.copy(configPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // 添加回滚动作
                rollbackActions.add(() -> {
                    try {
                        taskLogger.log(null, "回滚动作：还原adConfig文件", CreateNovelLogType.ERROR);
                        Files.copy(Paths.get(backupPath), configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(Paths.get(backupPath));
                    } catch (Exception ignore) {}
                });
                
                // 删除文件
                Files.deleteIfExists(configPath);
                
                // 操作成功后删除备份文件
                Files.deleteIfExists(Paths.get(backupPath));
                
                log.info("成功删除adConfig文件: {}", configFile);
            } else {
                // 如果不是最后一个平台，则只删除对应平台的配置块
                deletePlatformAdConfig(configPath, platform, rollbackActions);
            }
        } catch (Exception e) {
            log.error("删除adConfig文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除adConfig文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除指定平台的广告配置块
     * @param configPath 配置文件路径
     * @param platform 平台
     * @param rollbackActions 回滚动作列表
     */
    private void deletePlatformAdConfig(Path configPath, String platform, List<Runnable> rollbackActions) {
        try {
            // 读取原内容
            String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
            
            // 备份原文件
            String backupPath = configPath.toString() + ".bak";
            Files.copy(configPath, Paths.get(backupPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // 添加回滚动作
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(null, "回滚动作：还原adConfig文件", CreateNovelLogType.ERROR);
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
            
            log.info("成功清空adConfig文件中平台 {} 的配置: {}", platformKey, configPath.toString());
        } catch (Exception e) {
            log.error("清空adConfig文件中平台配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("清空adConfig文件中平台配置失败: " + e.getMessage(), e);
        }
    }
}
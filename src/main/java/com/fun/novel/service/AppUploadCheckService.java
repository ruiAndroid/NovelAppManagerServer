package com.fun.novel.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fun.novel.dto.AppAdWithConfigDTO;
import com.fun.novel.dto.AppPayWithConfigDTO;
import com.fun.novel.dto.AppUploadCheckDTO;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.service.impl.AppCommonConfigServiceImpl;
import com.fun.novel.service.impl.NovelAppServiceImpl;
import com.fun.novel.utils.NovelAppPublishUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AppUploadCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(AppUploadCheckService.class);
    
    @Autowired
    private AppCommonConfigService appCommonConfigService;
    
    @Autowired
    private NovelAppService novelAppService;
    
    @Autowired
    private AppPayService appPayService;
    
    @Autowired
    private AppAdService appAdService;
    
    @Value("${build.workPath}")
    private String buildWorkPath;
    
    /**
     * 执行命令并处理输出
     */
    private static class CommandResult {
        boolean success;
        String output;
        
        CommandResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }
    
    /**
     * 命令输出处理器接口
     */
    private interface CommandOutputHandler {
        void handleLine(String line);
    }
    
    /**
     * 执行命令并处理输出
     */
    private CommandResult executeCommand(String command, CommandOutputHandler outputHandler) {
        Process process = null;
        StringBuilder output = new StringBuilder();
        try {
            // 先设置控制台编码
            ProcessBuilder chcpBuilder = new ProcessBuilder("cmd.exe", "/c", "chcp 65001");
            Process chcpProcess = chcpBuilder.start();
            chcpProcess.waitFor();

            // 执行实际命令
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            // 重定向错误流到标准输出
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            // 处理输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 确保日志消息使用UTF-8编码
                    String logMessage = new String(line.getBytes(StandardCharsets.UTF_8), 
                                                 StandardCharsets.UTF_8);
                    output.append(logMessage).append("\n");
                    
                    if (outputHandler != null) {
                        outputHandler.handleLine(logMessage);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorMsg = "executeCommand fail 命令执行失败，退出码: " + exitCode;
                logger.error(errorMsg);
                return new CommandResult(false, output.toString());
            }
            return new CommandResult(true, output.toString());
        } catch (Exception e) {
            String errorMsg = "executeCommand error: " + e.getMessage();
            logger.error(errorMsg, e);
            return new CommandResult(false, output.toString() + "\n" + errorMsg);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
    
    /**
     * 执行发版前小程序检查
     */
    public AppUploadCheckDTO performUploadCheck(String appId,String projectPath) {
        try {
            logger.info("开始执行发版前小程序检查，appId: {}", appId);
            
            // 创建返回对象
            AppUploadCheckDTO result = new AppUploadCheckDTO();
            
            // 1. 获取AppCommonConfig以供后续使用
            AppCommonConfig appCommonConfig = appCommonConfigService.getAppCommonConfig(appId);
            if (appCommonConfig == null) {
                logger.warn("未找到应用的通用配置，appId: {}", appId);
                throw new RuntimeException("未找到应用的通用配置");
            }
            
            // 2. 检查是否有测试代码
            checkTestCode(appId, appCommonConfig, result);
            
            // 3. 获取线上版本号以及本地版本号数据
            getVersionInfo(appId,projectPath, appCommonConfig, result);
            
            // 4. 获取微距配置
            getMicroConfig(appId, result);
            
            // 5. 获取支付配置
            getPaymentConfig(appId, result);
            
            // 6. 获取广告配置
            getAdConfig(appId, result);
            
            logger.info("发版前小程序检查完成，appId: {}", appId);
            return result;
        } catch (Exception e) {
            logger.error("发版前小程序检查异常，appId: {}", appId, e);
            throw new RuntimeException("检查过程中发生异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否有测试代码
     */
    private void checkTestCode(String appId, AppCommonConfig appCommonConfig, AppUploadCheckDTO result) {
        try {
            logger.info("检查测试代码，appId: {}", appId);
            
            String buildCode = appCommonConfig.getBuildCode();
            if (buildCode == null || buildCode.isEmpty()) {
                logger.warn("应用的buildCode为空，appId: {}", appId);
                return;
            }
            
            // 2. 构建deliverConfigs配置文件路径
            String deliverConfigPath = buildWorkPath + "/src/modules/mod_config/deliverConfigs/" + buildCode + ".js";
            File deliverConfigFile = new File(deliverConfigPath);
            
            // 3. 检查文件是否存在
            if (!deliverConfigFile.exists()) {
                logger.warn("deliver配置文件不存在，路径: {}", deliverConfigPath);
                return;
            }
            
            // 4. 读取并解析配置文件
            String content = new String(Files.readAllBytes(deliverConfigFile.toPath()), StandardCharsets.UTF_8);
            
            // 5. 提取JSON部分
            String jsonPart = content.substring(content.indexOf("export default ") + "export default ".length());
            if (jsonPart.endsWith(";")) {
                jsonPart = jsonPart.substring(0, jsonPart.length() - 1);
            }
            
            // 6. 解析JSON配置
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置ObjectMapper以允许单引号
            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            // 配置ObjectMapper以允许不带引号的字段名
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            
            Map<String, Object> configMap = objectMapper.readValue(jsonPart, Map.class);
            
            // 7. 检查各平台的useTest字段
            boolean hasTestCode = false;
            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> platformConfig = (Map<String, Object>) entry.getValue();
                    Object useTestObj = platformConfig.get("useTest");
                    if (useTestObj instanceof Boolean && (Boolean) useTestObj) {
                        hasTestCode = true;
                        break;
                    }
                }
            }
            
            result.setHasTestCode(hasTestCode);
            logger.info("检查测试代码完成，appId: {}, hasTestCode: {}", appId, hasTestCode);
        } catch (Exception e) {
            logger.error("检查测试代码时发生异常，appId: {}", appId, e);
        }
    }
    
    /**
     * 获取线上版本号以及本地版本号数据
     */
    private void getVersionInfo(String appId,String projectPath, AppCommonConfig appCommonConfig, AppUploadCheckDTO result) {
        try {
            logger.info("获取版本信息，appId: {}", appId);
            
            // 1. 获取当前版本号（本地版本号）
            NovelApp novelApp = novelAppService.getByAppId(appId);
            if (novelApp == null) {
                logger.warn("未找到应用信息，appId: {}", appId);
                return;
            }
            
            // 2. 创建VersionInfo对象并设置当前版本号
            AppUploadCheckDTO.VersionInfo versionInfo = new AppUploadCheckDTO.VersionInfo();
            versionInfo.setCurrentVersion(novelApp.getVersion());
            
            // 3. 获取线上版本号
            String platform = novelApp.getPlatform();
            String onlineVersion = getOnlineVersion(appId,projectPath, platform, novelApp, appCommonConfig);
            versionInfo.setOnlineVersion(onlineVersion);
            
            // 4. 将VersionInfo设置到结果对象中
            result.setVersionInfo(versionInfo);
            
            logger.info("获取版本信息完成，appId: {}, currentVersion: {}, onlineVersion: {}", 
                       appId, novelApp.getVersion(), onlineVersion);
        } catch (Exception e) {
            logger.error("获取版本信息时发生异常，appId: {}", appId, e);
        }
    }
    
    /**
     * 获取线上版本号
     */
    private String getOnlineVersion(String appId,String projectPath, String platform, NovelApp novelApp, AppCommonConfig appCommonConfig) {
        try {
            logger.info("获取线上版本号，appId: {}, platform: {}", appId, platform);
            
            // 根据平台选择对应的处理器
            OnlineVersionHandler handler = getOnlineVersionHandler(platform);
            if (handler != null) {
                return handler.handle(appId, projectPath,novelApp, appCommonConfig);
            } else {
                logger.warn("不支持的平台，appId: {}, platform: {}", appId, platform);
                return null;
            }
        } catch (Exception e) {
            logger.error("获取线上版本号时发生异常，appId: {}, platform: {}", appId, platform, e);
            return null;
        }
    }
    
    /**
     * 获取线上版本号处理器
     */
    private OnlineVersionHandler getOnlineVersionHandler(String platform) {
        switch (platform) {
            case "douyin":
                return new DouyinOnlineVersionHandler();
            case "kuaishou":
                return new KuaishouOnlineVersionHandler();
            case "weixin":
                return new WeixinOnlineVersionHandler();
            default:
                return null;
        }
    }
    
    /**
     * 线上版本号处理器接口
     */
    private interface OnlineVersionHandler {
        String handle(String appId,String projectPath, NovelApp novelApp, AppCommonConfig appCommonConfig);
    }
    
    /**
     * 抖音平台线上版本号处理器
     */
    private class DouyinOnlineVersionHandler implements OnlineVersionHandler {

        @Override
        public String handle(String appId,String projectPath, NovelApp novelApp, AppCommonConfig appCommonConfig) {
            try {
                logger.info("获取抖音平台线上版本号，appId: {}", appId);
                
                // 构建设置token命令
                String douyinAppToken = appCommonConfig.getDouyinAppToken();
                if (douyinAppToken == null || douyinAppToken.isEmpty()) {
                    logger.warn("抖音平台token为空，appId: {}", appId);
                    return null;
                }
                
                String tokenCmd = String.format("tma set-app-config %s --token %s", appId, douyinAppToken);
                logger.info("执行抖音设置token命令: {}", tokenCmd);
                
                // 执行设置token命令
                CommandResult tokenResult = executeCommand(tokenCmd, null);
                if (!tokenResult.success) {
                    logger.error("执行抖音设置token命令失败，appId: {}, 输出: {}", appId, tokenResult.output);
                    return null;
                }
                
                // 构建获取线上版本号命令
                String versionCmd = String.format("tma get-meta %s", appId);
                logger.info("执行抖音获取线上版本号命令: {}", versionCmd);
                
                // 用于存储解析出的版本号
                final String[] onlineVersion = {null};
                
                // 执行获取版本号命令并解析结果
                CommandResult versionResult = executeCommand(versionCmd, line -> {
                    // 解析返回的JSON数据 {"version":"4.1.7"}
                    if (line.contains("\"version\"")) {
                        // 使用正则表达式提取版本号
                        Pattern pattern = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            onlineVersion[0] = matcher.group(1);
                        }
                    }
                });
                
                if (!versionResult.success) {
                    logger.error("执行抖音获取线上版本号命令失败，appId: {}, 输出: {}", appId, versionResult.output);
                    return null;
                }
                
                logger.info("成功获取抖音平台线上版本号，appId: {}, version: {}", appId, onlineVersion[0]);
                return onlineVersion[0];
            } catch (Exception e) {
                logger.error("获取抖音平台线上版本号时发生异常，appId: {}", appId, e);
                return null;
            }
        }
    }
    
    /**
     * 快手平台线上版本号处理器
     */
    private class KuaishouOnlineVersionHandler implements OnlineVersionHandler {

        @Override
        public String handle(String appId,String projectPath, NovelApp novelApp, AppCommonConfig appCommonConfig) {
            try {
                logger.info("获取快手平台线上版本号，appId: {}", appId);
                // 构建设置token命令
                String kuaishouAppToken = appCommonConfig.getKuaishouAppToken();
                if (kuaishouAppToken == null || kuaishouAppToken.isEmpty()) {
                    logger.warn("快手平台token为空，appId: {}", appId);
                    return null;
                }

                try {
                    // 使用现有的方法生成密钥文件
                    buildKuaishouKeyFile(appId, projectPath, kuaishouAppToken);

                } catch (Exception e) {
                    String errorMsg = "[快手] 密钥文件生成失败: " + e.getMessage();
                    logger.error(errorMsg);
                    return null;
                }


                // 构建获取线上版本号命令
                // 构建密钥文件路径
                File keyFile = new File(projectPath, "private." + appId + ".key");
                if (!keyFile.exists()) {
                    throw new RuntimeException("密钥文件不存在: " + keyFile.getAbsolutePath());
                }
                String versionCmd = String.format("ks-miniprogram-ci appinfo --pp %s --pkp %s --appid %s",
                        projectPath,
                        keyFile.getAbsolutePath(),
                        appId);
                logger.info("执行快手获取线上版本号命令: {}", versionCmd);


                // 执行获取版本号命令并解析结果
                CommandResult versionResult = executeCommand(versionCmd, line -> {

                });
                if (!versionResult.success) {
                    logger.error("执行快手获取线上版本号命令失败，appId: {}, 输出: {}", appId, versionResult.output);
                    return null;
                }
                return null;
            } catch (Exception e) {
                logger.error("获取快手平台线上版本号时发生异常，appId: {}", appId, e);
                return null;
            }
        }
    }

    /**
     * 快手平台：生成密钥文件
     */
    private void buildKuaishouKeyFile(String appId, String projectPath, String kuaishouAppToken) {
        // 构建密钥文件路径
        File keyFile = new File(projectPath, "private." + appId + ".key");

        // 确保父目录存在
        if (!keyFile.getParentFile().exists()) {
            keyFile.getParentFile().mkdirs();
        }

        // 写入token内容到文件，使用UTF-8编码
        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(keyFile), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(kuaishouAppToken);
        } catch (Exception e) {
            String errorMsg = "生成快手密钥文件失败: " + e.getMessage();
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        logger.info("快手密钥文件生成成功: {}", keyFile.getAbsolutePath());
    }


    /**
     * 微信平台线上版本号处理器
     */
    private class WeixinOnlineVersionHandler implements OnlineVersionHandler {

        @Override
        public String handle(String appId,String projectPath, NovelApp novelApp, AppCommonConfig appCommonConfig) {
            try {
                logger.info("获取微信平台线上版本号，appId: {}", appId);

                return null;
            } catch (Exception e) {
                logger.error("获取微信平台线上版本号时发生异常，appId: {}", appId, e);
                return null;
            }
        }
    }


    /**
     * 获取微距配置
     */
    private void getMicroConfig(String appId, AppUploadCheckDTO result) {
        try {
            logger.info("获取微距配置，appId: {}", appId);
            
            // 通过novelAppService获取应用信息
            NovelApp novelApp = novelAppService.getByAppId(appId);
            if (novelApp == null) {
                logger.warn("未找到应用信息，appId: {}", appId);
                return;
            }
            
            // 创建DeliverInfo对象并设置deliverId和bannerId
            AppUploadCheckDTO.DeliverInfo deliverInfo = new AppUploadCheckDTO.DeliverInfo();
            deliverInfo.setDeliverId(novelApp.getDeliverId());
            deliverInfo.setBannerId(novelApp.getBannerId());
            
            // 将DeliverInfo设置到结果对象中
            result.setDeliverInfo(deliverInfo);
            
            logger.info("获取微距配置完成，appId: {}, deliverId: {}, bannerId: {}", 
                       appId, novelApp.getDeliverId(), novelApp.getBannerId());
        } catch (Exception e) {
            logger.error("获取微距配置时发生异常，appId: {}", appId, e);
        }
    }
    
    /**
     * 获取支付配置
     */
    private void getPaymentConfig(String appId, AppUploadCheckDTO result) {
        try {
            logger.info("获取支付配置，appId: {}", appId);
            
            // 通过appPayService获取支付配置信息
            AppPayWithConfigDTO appPayWithConfig = appPayService.getAppPayByAppId(appId);
            if (appPayWithConfig == null) {
                logger.info("未找到支付配置信息，appId: {}", appId);
                return;
            }
            
            // 将支付配置信息设置到结果对象中
            result.setAppPayWithConfig(appPayWithConfig);
            
            logger.info("获取支付配置完成，appId: {}", appId);
        } catch (Exception e) {
            logger.error("获取支付配置时发生异常，appId: {}", appId, e);
        }
    }
    
    /**
     * 获取广告配置
     */
    private void getAdConfig(String appId, AppUploadCheckDTO result) {
        try {
            logger.info("获取广告配置，appId: {}", appId);
            
            // 通过appAdService获取广告配置信息
            AppAdWithConfigDTO appAdWithConfig = appAdService.getAppAdByAppId(appId);
            if (appAdWithConfig == null) {
                logger.info("未找到广告配置信息，appId: {}", appId);
                return;
            }
            
            // 将广告配置信息设置到结果对象中
            result.setAppAdWithConfig(appAdWithConfig);
            
            logger.info("获取广告配置完成，appId: {}", appId);
        } catch (Exception e) {
            logger.error("获取广告配置时发生异常，appId: {}", appId, e);
        }
    }
}
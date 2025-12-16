package com.fun.novel.utils;

import com.fun.novel.entity.UserTaskLog;
import com.fun.novel.service.UserTaskLogService;
import com.fun.novel.service.impl.TaskManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;

@Component
public class NovelAppPublishUtil {
    private static final Logger logger = LoggerFactory.getLogger(NovelAppPublishUtil.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private PublishTaskManager publishTaskManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TaskManagementService taskManagementService;

    @Autowired
    private UserTaskLogService userTaskLogService;

    /**
     * 发布小程序到指定平台
     */
    public String publishNovelApp(String platformCode, String appId, String projectPath, String douyinAppToken, String kuaishouAppToken,String weixinAppToken,String baiduAppToken, String version, String log) {
        // 创建任务，检查平台是否已有任务在运行
        String taskId = publishTaskManager.createTask(platformCode);
        if (taskId == null) {
            String runningTaskId = publishTaskManager.getPlatformRunningTask(platformCode);
            logger.warn("平台 {} 已有任务 {} 在运行中，请稍后再试", platformCode, runningTaskId);
            return null;
        }

        // 设置项目路径
        publishTaskManager.setProjectPath(taskId, projectPath);



        // 使用异步方式执行发布任务
        CompletableFuture.runAsync(() -> {
            try {
                // 给前端一些时间建立WebSocket连接
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
            // 发送任务开始消息
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "开始发布任务...");
            logger.info("开始发布任务: {}", taskId);
            Process process = null;
            try {
                // 检查项目路径
                File projectDir = new File(projectPath);
                if (!projectDir.exists() || !projectDir.isDirectory()) {
                    throw new RuntimeException("项目路径不存在或不是目录: " + projectPath);
                }

                // 创建 ProcessBuilder，供所有平台使用
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.directory(projectDir);
                processBuilder.redirectErrorStream(true);

                // 根据平台选择对应的发布处理器
                PlatformPublishHandler handler = getPlatformHandler(platformCode);
                if (handler != null) {
                    handler.handlePublish(taskId, appId, projectPath, douyinAppToken, kuaishouAppToken,weixinAppToken,baiduAppToken, version, log, processBuilder);

                } else {
                    String errorMsg = "发布过程发生错误: handler is null ";
                    logger.error(errorMsg);
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error");
                }
            } catch (Exception e) {
                String errorMsg = "发布过程发生错误: " + e.getMessage();
                logger.error(errorMsg);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error " + errorMsg);
            } finally {
                // 发送任务完成信号，让前端知道可以断开连接
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "TASK_COMPLETED");
                // 延迟清理任务，确保前端能接收到所有消息
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(3000); // 等待5秒确保消息发送完成
                    } catch (InterruptedException ignored) {
                    }
                    publishTaskManager.removeTask(taskId);
                });
            }
        }, executorService);

        return taskId;
    }

    /**
     * 生成指定小程序的预览码
     */
    public String previewQrCode(Long userId,
                                String platformCode,
                                String appId,
                                String projectPath,
                                String version,
                                String douyinAppToken,
                                String kuaishouAppToken,
                                String weixinAppToken,
                                String baiduAppToken,
                                String path,
                                String query,
                                String scene
                                ) {
        // 创建任务，检查平台是否已有任务在运行
        String taskId = publishTaskManager.createTask(platformCode);
        if (taskId == null) {
            String runningTaskId = publishTaskManager.getPlatformRunningTask(platformCode);
            logger.warn("平台 {} 已有任务 {} 在运行中，请稍后再试", platformCode, runningTaskId);
            return null;
        }

        // 设置项目路径
        publishTaskManager.setProjectPath(taskId, projectPath);


        //记录用户任务开始
//        Long mainTaskId = taskManagementService.createTaskWithSubTasks(userId, appId, platformCode, "PREVIEW", "生成预览码任务");


        // 记录任务日志
//        userTaskLogService.createTaskLog(mainTaskId, "PREVIEW", "INFO", "开始生成预览码任务","");

        // 使用异步方式执行发布任务
        CompletableFuture.runAsync(() -> {
            try {
                // 给前端一些时间建立WebSocket连接
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
            // 发送生成预览码开始消息
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "开始生成预览码...");

            // 记录任务日志
//            userTaskLogService.createTaskLog(mainTaskId, "PREVIEW", "START", "开始生成预览码...","");
            
            Process process = null;
            try {
                // 检查项目路径
                File projectDir = new File(projectPath);
                if (!projectDir.exists() || !projectDir.isDirectory()) {
                    throw new RuntimeException("项目路径不存在或不是目录: " + projectPath);
                }

                // 创建 ProcessBuilder，供所有平台使用
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.directory(projectDir);
                processBuilder.redirectErrorStream(true);

                // 根据平台选择对应的处理器
                PlatformPublishHandler handler = getPlatformHandler(platformCode);
                if (handler != null) {
                    handler.handlePreview(taskId, appId, projectPath,version, douyinAppToken, kuaishouAppToken,weixinAppToken,baiduAppToken,path, query, scene, processBuilder);
                } else {
                    String errorMsg = "生成预览码过程发生错误: handler is null ";
                    logger.error(errorMsg);
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error");
                    
                    // 记录任务日志
//                    userTaskLogService.createTaskLog(mainTaskId, "PREVIEW", "ERROR", errorMsg,"");
                }
            } catch (Exception e) {
                String errorMsg = "生成预览码过程发生错误: " + e.getMessage();
                logger.error(errorMsg);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error " + errorMsg);

                // 记录任务日志
//                userTaskLogService.createTaskLog(mainTaskId, "PREVIEW", "ERROR", errorMsg,"");
            } finally {
                // 发送任务完成信号，让前端知道可以断开连接
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "TASK_COMPLETED");

                // 记录任务完成日志
//                userTaskLogService.createTaskLog(mainTaskId, "PREVIEW", "INFO", "预览码生成任务完成","");

                // 延迟清理任务，确保前端能接收到所有消息
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(3000); // 等待5秒确保消息发送完成
                    } catch (InterruptedException ignored) {
                    }
                    publishTaskManager.removeTask(taskId);
                });
            }
        }, executorService);

        return taskId;

    }

        /**
         * 获取平台对应的发布处理器
         */
    private PlatformPublishHandler getPlatformHandler(String platformCode) {
        switch (platformCode) {
            case "mp-toutiao":
                return new DouyinPublishHandler();
            case "mp-kuaishou":
                return new KuaishouPublishHandler();
            case "mp-weixin":
                return new WeixinPublishHandler();
            case "mp-baidu":
                return new BaiduPublishHandler();
            default:
                return null;
        }
    }

    /**
     * 执行命令并处理输出
     */
    private boolean executeCommand(String taskId, String command, ProcessBuilder processBuilder, CommandOutputHandler outputHandler) {
        Process process = null;
        try {
            // 先设置控制台编码
            ProcessBuilder chcpBuilder = new ProcessBuilder("cmd.exe", "/c", "chcp 65001");
            Process chcpProcess = chcpBuilder.start();
            chcpProcess.waitFor();

            // 执行实际命令
            processBuilder.command("cmd.exe", "/c", command);
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            // 重定向错误流到标准输出
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            publishTaskManager.addTask(taskId, process);

            // 处理输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 确保日志消息使用UTF-8编码
                    String logMessage = new String(line.getBytes(java.nio.charset.StandardCharsets.UTF_8), 
                                                 java.nio.charset.StandardCharsets.UTF_8);
                    
                    // 发送消息到前端
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, logMessage);
                    
                    // 记录到日志
                    if (logMessage.contains("Error") || logMessage.contains("error") || logMessage.contains("Warning")) {
                        logger.warn("Command output for task {}: {}", taskId, logMessage);
                    } else {
                        logger.info("Command output for task {}: {}", taskId, logMessage);
                    }
                    if(logMessage.contains("Upload Error")){ //TODO 抖快微的报错log都不一致，碰到一个加一个
                        return false;
                    }

                    if (outputHandler != null) {
                        try {
                            outputHandler.handleLine(logMessage);
                        } catch (RuntimeException e) {
                            if (e.getMessage().contains("success")) {
                                return true;
                            }
                            throw e;
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorMsg = "executeCommand fail 命令执行失败，退出码: " + exitCode;
                logger.error(errorMsg);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, errorMsg);
                return false;
            }
            return true;
        } catch (Exception e) {
            String errorMsg = "executeCommand error" + e.getMessage();
            logger.error(errorMsg);
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, errorMsg);
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 平台发布处理器接口
     */
    private interface PlatformPublishHandler {
        void handlePublish(String taskId, String appId, String projectPath, String douyinAppToken, String kuaishouAppToken,String weixinAppToken,String baiduAppToken, String version, String log, ProcessBuilder processBuilder);
        void handlePreview(String taskId, String appId, String projectPath,String version, String douyinAppToken, String kuaishouAppToken,String weixinAppToken,String baiduAppToken, String path, String query, String scene, ProcessBuilder processBuilder);
    }

    /**
     * 命令输出处理器接口
     */
    private interface CommandOutputHandler {
        void handleLine(String line);
    }

    /**
     * 抖音平台发布处理器
     */
    private class DouyinPublishHandler implements PlatformPublishHandler {
        @Override
        public void handlePublish(String taskId, String appId, String projectPath, String douyinAppToken, String kuaishouAppToken, String weixinAppToken, String baiduAppToken,String version, String log, ProcessBuilder processBuilder) {
            // 步骤1：设置token
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] 开始设置Token...");
            String tokenCmd = buildDouyinTokenCommand(appId, douyinAppToken);
            logger.info("[抖音] tokenCmd :{}",tokenCmd);
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] tokenCmd :"+tokenCmd);
            boolean publishExecuteCommandResult = executeCommand(taskId, tokenCmd, processBuilder, line -> {});
            if(!publishExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [抖音]设置小程序Token失败");
                return;
            }

            // 步骤2：上传
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] 开始上传小程序...");
            String uploadCmd = buildDouyinUploadCommand(version, log, projectPath);
            boolean uploadExecuteCommandResult = executeCommand(taskId, uploadCmd, processBuilder, line -> {});
            if(!uploadExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [抖音]上传小程序失败");
                return;
            }

            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音]上传小程序成功");

            // 步骤3：生成二维码
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] 开始生成二维码...");
            String previewCmd = buildDouyinPreviewCommand(projectPath);
            boolean previewExecuteCommandResult = executeCommand(taskId, previewCmd, processBuilder, line -> {
                if (line.contains("二维码信息：")) {
                    String qrCodeUrl = line.substring(line.indexOf("二维码信息：") + 6);
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] 二维码生成成功: " + qrCodeUrl);
                }
            });
            if(!previewExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [抖音] 二维码生成失败");
                return;
            }

            String completeMsg = "Publish success [抖音] 发布流程全部完成";
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, completeMsg);
        }

        @Override
        public void handlePreview(String taskId, String appId, String projectPath,String version, String douyinAppToken, String kuaishouAppToken, String weixinAppToken, String baiduAppToken, String path, String query, String scene, ProcessBuilder processBuilder) {
            // 步骤1：设置token
            try {
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] 开始设置Token...");

                String tokenCmd = buildDouyinTokenCommand(appId, douyinAppToken);
                logger.info("[抖音] tokenCmd :{}",tokenCmd);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] tokenCmd :"+tokenCmd);

                boolean tokenExecuteCommandResult = executeCommand(taskId, tokenCmd, processBuilder, line -> {});
                if(!tokenExecuteCommandResult){
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error [抖音]设置小程序Token失败");
                    return;
                }
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] Token设置成功");
            } catch (Exception e) {
                String errorMsg = "[抖音] Token设置失败: " + e.getMessage();
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error [抖音] Token设置失败:" + errorMsg);
                return;
            }

            logger.info("[抖音] 开始生成二维码...");

            // 步骤2：生成二维码
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[抖音] 开始生成二维码...");
            String previewCmd = buildDouyinAppointQrCodePreviewCommand(projectPath,path,query,scene);
            logger.info("[抖音] previewCmd :{}",previewCmd);

            boolean previewExecuteCommandResult = executeCommand(taskId, previewCmd, processBuilder, line -> {
                if (line.contains("二维码信息") ) {
                    String qrCodeUrl = line.substring(line.indexOf("二维码信息：") + 6);
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode success [抖音] 二维码生成成功: " + qrCodeUrl);
                }
            });
            logger.info("[抖音] 二维码生成完成 previewExecuteCommandResult："+previewExecuteCommandResult);
            if(!previewExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error [抖音] 二维码生成失败");
            }
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode success [抖音] 生成二维码完成");

        }
    }

    /**
     * 快手平台发布处理器
     */
    private class KuaishouPublishHandler implements PlatformPublishHandler {
        @Override
        public void handlePublish(String taskId, String appId, String projectPath, String douyinAppToken, String kuaishouAppToken,String weixinAppToken, String baiduAppToken, String version, String log, ProcessBuilder processBuilder) {
            // 步骤1：生成密钥
            try {
                // 发送开始生成密钥的消息
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 开始生成密钥文件...");

                // 使用现有的方法生成密钥文件
                buildKuaishouKeyFile(appId, projectPath, kuaishouAppToken);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 密钥文件生成成功");

            } catch (Exception e) {
                String errorMsg = "[快手] 密钥文件生成失败: " + e.getMessage();
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [快手] 密钥文件生成失败:" + errorMsg);
                return;
            }

            logger.info("[快手] 开始发布小程序...");

            //步骤2：发布
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 开始发布小程序...");

            String uploadCmd = buildKuaishouUploadCommand(appId, projectPath, version, log);
            logger.info("[快手] uploadCmd :{}",uploadCmd);
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] uploadCmd :"+uploadCmd);

            boolean publishExecuteCommandResult=executeCommand(taskId, uploadCmd, processBuilder, line -> {
                if (line.contains("[ks upload]  done.")) {
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 小程序发布成功");
                }
            });
            logger.info("[快手] publishExecuteCommandResult :{}",publishExecuteCommandResult);
            if(!publishExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [快手]上传小程序失败");
                return;

            }

            // 步骤3：生成二维码
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 开始生成二维码...");

            String previewCmd = buildKuaishouPreviewCommand(appId, projectPath);
            boolean previewExecuteCommandResult = executeCommand(taskId, previewCmd, processBuilder, line -> {
                if (line.contains("[ks preview]  done.")) {
                    String qrcodePath = projectPath + "\\ks_qrcode.png";
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 二维码生成成功: " + qrcodePath);

                }
            });
            if(!previewExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [快手] 二维码生成失败");
            }

            String completeMsg = "Publish success [快手] 发布流程全部完成";
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, completeMsg);
        }

        @Override
        public void handlePreview(String taskId, String appId, String projectPath,String version, String douyinAppToken, String kuaishouAppToken, String weixinAppToken,  String baiduAppToken,String path, String query, String scene, ProcessBuilder processBuilder) {
            // 步骤1：生成密钥
            try {
                // 发送开始生成密钥的消息
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 开始生成密钥文件...");

                // 使用现有的方法生成密钥文件
                buildKuaishouKeyFile(appId, projectPath, kuaishouAppToken);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 密钥文件生成成功");

            } catch (Exception e) {
                String errorMsg = "[快手] 密钥文件生成失败: " + e.getMessage();
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [快手] 密钥文件生成失败:" + errorMsg);
                return;
            }
            logger.info("[快手] 开始生成二维码...");

            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[快手] 开始生成二维码...");
            String previewCmd = buildKuaishouAppointQrCodePreviewCommand(appId,projectPath,path,query,scene);
            logger.info("[快手] previewCmd :{}",previewCmd);

            boolean previewExecuteCommandResult = executeCommand(taskId, previewCmd, processBuilder, line -> {
                if (line.contains("[ks preview]  done.")) {
                    String qrcodePath = projectPath + "\\ks_qrcode.png";
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode success [快手] 二维码生成成功: " + qrcodePath);

                }
            });
            logger.info("[快手] 二维码生成完成 previewExecuteCommandResult："+previewExecuteCommandResult);
            if(!previewExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error [快手] 二维码生成失败");
            }
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode success [快手] 生成二维码完成");

        }
    }

    /**
     * 微信平台发布处理器
     */
    private class WeixinPublishHandler implements PlatformPublishHandler {
        @Override
        public void handlePublish(String taskId, String appId, String projectPath, String douyinAppToken, String kuaishouAppToken,String weixinAppToken,  String baiduAppToken,String version, String log, ProcessBuilder processBuilder) {
            // 步骤1：生成密钥
            try {
                // 发送开始生成密钥的消息
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] 开始生成密钥文件...");

                // 使用现有的方法生成密钥文件
                buildWeixinKeyFile(appId, projectPath, weixinAppToken);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] 密钥文件生成成功");

            } catch (Exception e) {
                String errorMsg = "[微信] 密钥文件生成失败: " + e.getMessage();
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [微信] 密钥文件生成失败:" + errorMsg);
                return;
            }
            logger.info("[微信] 开始发布小程序...");

            //步骤2：发布
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] 开始发布小程序...");

            String publishCmd = buildWeixinUploadCommand(appId, projectPath,version,log);
            logger.info("[微信] publishCmd :{}",publishCmd);
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] publishCmd :"+publishCmd);

            boolean publishExecuteCommandResult=executeCommand(taskId, publishCmd, processBuilder, line -> {});
            logger.info("[微信] publishExecuteCommandResult :{}",publishExecuteCommandResult);
            if(!publishExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [微信]发布小程序失败");
                return;
            }
            //步骤3：预览生成二维码
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] 开始生成二维码...");
            String previewCmd = buildWeixinPreviewCommand(appId, projectPath,version,log);



            boolean previewExecuteCommandResult=executeCommand(taskId, previewCmd, processBuilder, line -> {
                if (line.contains("\"message\":\"upload\",\"status\":\"done\"")) {
                    String qrcodePath = projectPath + "\\wx_qrcode.png";
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] 二维码生成成功: "+ qrcodePath);
                }
            });
            if(!previewExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Publish error [微信]预览二维码失败");
                return;
            }

            String completeMsg = "Publish success [微信] 发布流程全部完成";
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, completeMsg);


        }

        @Override
        public void handlePreview(String taskId, String appId, String projectPath,String version, String douyinAppToken, String kuaishouAppToken, String weixinAppToken,  String baiduAppToken,String path, String query, String scene, ProcessBuilder processBuilder) {
            // 步骤1：生成密钥
            try {
                // 发送开始生成密钥的消息
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] 开始生成密钥文件...");

                // 使用现有的方法生成密钥文件
                buildWeixinKeyFile(appId, projectPath, weixinAppToken);
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] 密钥文件生成成功");

            } catch (Exception e) {
                String errorMsg = "[微信] 密钥文件生成失败: " + e.getMessage();
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error [微信] 密钥文件生成失败:" + errorMsg);
                return;
            }
            logger.info("[微信] 开始生成预览码...");

            // 步骤2：生成二维码
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[微信] 开始生成二维码...");
            String previewCmd = buildWeixinAppointQrCodePreviewCommand(appId,projectPath,version, path,query,scene);
            logger.info("[微信] previewCmd :{}",previewCmd);

            boolean previewExecuteCommandResult = executeCommand(taskId, previewCmd, processBuilder, line -> {
                if (line.contains("\"message\":\"upload\",\"status\":\"done\"")) {
                    String qrcodePath = projectPath + "\\wx_qrcode.png";
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode success [微信] 二维码生成成功: "+ qrcodePath);
                }
            });
            if(!previewExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error [微信] 二维码生成失败");
            }


            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode success [微信] 生成二维码完成");


        }
    }

    private class BaiduPublishHandler implements PlatformPublishHandler {

        @Override
        public void handlePublish(String taskId, String appId, String projectPath, String douyinAppToken, String kuaishouAppToken, String weixinAppToken, String baiduAppToken, String version, String log, ProcessBuilder processBuilder) {

        }

        @Override
        public void handlePreview(String taskId, String appId, String projectPath, String version, String douyinAppToken, String kuaishouAppToken, String weixinAppToken, String baiduAppToken, String path, String query, String scene, ProcessBuilder processBuilder) {
            logger.info("[百度] 开始生成预览码...");
            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "[百度] 开始生成二维码...");
            String previewCmd = buildBaiduAppointQrCodePreviewCommand(appId,projectPath,version,baiduAppToken, path,query,scene);
            logger.info("[百度] previewCmd :{}",previewCmd);
            boolean previewExecuteCommandResult = executeCommand(taskId, previewCmd, processBuilder, line -> {
//                logger.info("[百度] previewCmd line:{}",line);

                if (line.contains("url")) {
                    String qrCodeUrl = line.substring(line.indexOf("url"));
                    messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode success [百度] 二维码生成成功: " + qrCodeUrl);
                }
            });
            if(!previewExecuteCommandResult){
                messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode error [百度] 二维码生成失败");
            }


            messagingTemplate.convertAndSend("/topic/publish-logs/" + taskId, "Preview QrCode success [百度] 生成二维码完成");

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
     * 微信平台：生成密钥文件
     */
    private void buildWeixinKeyFile(String appId, String projectPath, String weixinAppToken) {
        // 构建密钥文件路径
        File keyFile = new File(projectPath, "private." + appId + ".key");

        // 确保父目录存在
        if (!keyFile.getParentFile().exists()) {
            keyFile.getParentFile().mkdirs();
        }

        // 写入token内容到文件，使用UTF-8编码
        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(keyFile), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(weixinAppToken);
        } catch (Exception e) {
            String errorMsg = "生成微信密钥文件失败: " + e.getMessage();
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        logger.info("微信密钥文件生成成功: {}", keyFile.getAbsolutePath());
    }

    /**
     * 构建抖音token设置命令
     */
    private String buildDouyinTokenCommand(String appId, String douyinAppToken) {
        return String.format("tma set-app-config %s --token %s", appId, douyinAppToken);
    }

    /**
     * 构建抖音上传命令
     */
    private String buildDouyinUploadCommand(String version, String log, String projectPath) {
        return String.format("tma upload -v %s -c \"%s\" %s", version, log, projectPath);
    }

    /**
     * 构建抖音预览命令
     */
    private String buildDouyinPreviewCommand(String projectPath) {
        return "tma preview " + projectPath;
    }

    /**
     * 生成抖音指定路径和参数的预览命令
     *
     */
    private String buildDouyinAppointQrCodePreviewCommand(String projectPath,String path,String query,String scene){
        // 确保参数不为空，避免命令执行时报错
        String safePath = (path != null) ? path : "";
        String safeQuery = (query != null) ? query : "";
        String safeScene = (scene != null) ? scene : "";
        
        // 对参数进行转义以防止特殊字符引起问题
        safePath = safePath.replace("\"", "\\\"");
        safeQuery = safeQuery.replace("\"", "\\\"");
        
        return String.format(
                "tma preview --miniapp-path \"%s\" --miniapp-query \"%s\" --miniapp-scene %s  %s",
                safePath,
                safeQuery,
                safeScene,
                projectPath
        );
    }
    /**
     * 快手平台：发布命令
     */
    private String buildKuaishouUploadCommand(String appId, String projectPath, String version, String log) {
        // 构建密钥文件路径
        File keyFile = new File(projectPath, "private." + appId + ".key");
        if (!keyFile.exists()) {
            throw new RuntimeException("密钥文件不存在: " + keyFile.getAbsolutePath());
        }

        // 构建发布命令
        return String.format(
            "ks-miniprogram-ci upload --pp %s --appid %s --pkp %s --uv %s --ud \"%s\"",
            projectPath,
            appId,
            keyFile.getAbsolutePath(),
            version,
            log
        );
    }

    /**
     * 快手平台：生成二维码命令
     */
    private String buildKuaishouPreviewCommand(String appId, String projectPath) {
        // 构建生成二维码命令
        String qrcodePath = projectPath + "\\ks_qrcode.png";
        return String.format(
            "ks-miniprogram-ci preview --pp %s --appid %s --pkp %s --qrcode-format image --qrcode-output-dest %s",
            projectPath,
            appId,
            projectPath + "\\private." + appId + ".key",
            qrcodePath);
    }


    private String buildKuaishouAppointQrCodePreviewCommand(String appId, String projectPath, String path, String query, String scene) {
        // 构建密钥文件路径
        File keyFile = new File(projectPath, "private." + appId + ".key");
        if (!keyFile.exists()) {
            throw new RuntimeException("密钥文件不存在: " + keyFile.getAbsolutePath());
        }

        String qrcodePath = projectPath + "\\ks_qrcode.png";

        return String.format(
                "ks-miniprogram-ci preview --pp %s --appid %s --pkp %s --qrcode-format image  --preview-page-path %s --preview-search-query %s --scene %s --qrcode-output-dest %s ",
                projectPath,
                appId,
                projectPath + "\\private." + appId + ".key",
                path,
                query,
                scene,
                qrcodePath);

    }
    /**
     * 微信平台：发布命令
     */
    private String buildWeixinUploadCommand(String appId, String projectPath, String version, String log) {
        // 构建密钥文件路径
        File keyFile = new File(projectPath, "private." + appId + ".key");
        if (!keyFile.exists()) {
            throw new RuntimeException("密钥文件不存在: " + keyFile.getAbsolutePath());
        }

        // 构建发布命令
        return String.format(
                "miniprogram-ci upload --pp %s --appid %s --pkp %s --uv %s --ud %s --enable-es6 true --enable-es7 true --enable-minify-wxss true --enable-minify-js true --enable-minify-wxml true --enable-minify true",
                projectPath,
                appId,
                keyFile.getAbsolutePath(),
                version,
                log
        );
    }

    /**
     * 微信平台：预览生成二维码命令
     */
    private String buildWeixinPreviewCommand(String appId, String projectPath, String version, String log) {
        // 构建密钥文件路径
        File keyFile = new File(projectPath, "private." + appId + ".key");
        if (!keyFile.exists()) {
            throw new RuntimeException("密钥文件不存在: " + keyFile.getAbsolutePath());
        }
        String qrcodePath = projectPath + "\\wx_qrcode.png";

        // 构建发布命令
        return String.format(
                "miniprogram-ci preview --pp %s --appid %s --pkp %s --uv %s --ud %s --enable-es6 true --enable-es7 true --enable-minify-wxss true --enable-minify-js true --enable-minify-wxml true --enable-minify true  --qrcode-format image --qrcode-output-dest %s",
                projectPath,
                appId,
                keyFile.getAbsolutePath(),
                version,
                log,qrcodePath
        );
    }

    private String buildWeixinAppointQrCodePreviewCommand(String appId, String projectPath,String version, String path, String query, String scene) {
        // 构建密钥文件路径
        File keyFile = new File(projectPath, "private." + appId + ".key");
        if (!keyFile.exists()) {
            throw new RuntimeException("密钥文件不存在: " + keyFile.getAbsolutePath());
        }
        String qrcodePath = projectPath + "\\wx_qrcode.png";

        // 构建发布命令
        return String.format(
                "miniprogram-ci preview --pp %s --appid %s --pkp %s --uv %s --ud %s --enable-es6 true --enable-es7 true --enable-minify-wxss true --enable-minify-js true --enable-minify-wxml true --enable-minify true  --qrcode-format image --preview-page-path %s --preview-search-query %s --scene %s --qrcode-output-dest %s",
                projectPath,
                appId,
                keyFile.getAbsolutePath(),
                version,
                "test",
                path,
                query,
                scene,
                qrcodePath
        );
    }

    private String buildBaiduAppointQrCodePreviewCommand(String appId, String projectPath,String version,String baiduAppToken, String path, String query, String scene) {
        logger.info("百度平台：生成预览二维码命令 projectPath:"+projectPath);
        // 构建发布命令
        return String.format("swan preview --project-path \"%s\" --min-swan-version %s --index-page %s --token %s  --json --verbose --force-use-new-compiler",
                projectPath,
                "4.540.1",
                path,
                baiduAppToken);
    }

    /**
     * 停止发布任务
     */
    public void stopPublish(String taskId) {
        publishTaskManager.stopTask(taskId);
    }


    @PreDestroy
    public void cleanup() {
        logger.info("关闭发布工具线程池...");
        executorService.shutdown();
    }
}
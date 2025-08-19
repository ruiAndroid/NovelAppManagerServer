package com.fun.novel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.dto.NovelAppBuildInfoDTO;
import com.fun.novel.dto.NovelAppPublishDTO;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.NovelAppService;
import com.fun.novel.utils.NovelAppBuildUtil;
import com.fun.novel.utils.NovelAppPublishUtil;
import com.fun.novel.utils.PublishTaskManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/novel-publish")
@Tag(name = "小程序发布", description = "小程序发布相关接口")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://172.17.5.80:5173",
    "http://172.17.5.80:8080"
}, allowCredentials = "true")
public class NovelAppPublishController {
    private static final Logger logger = LoggerFactory.getLogger(NovelAppPublishController.class);

    @Autowired
    private NovelAppBuildUtil novelAppBuildUtil;

    @Autowired
    private NovelAppPublishUtil novelAppPublishUtil;

    @Autowired
    private NovelAppService novelAppService;

    @Autowired
    private PublishTaskManager publishTaskManager;

    private static final Map<String, String> PLATFORM_NAMES = new HashMap<>();
    static {
        PLATFORM_NAMES.put("mp-toutiao", "抖音小程序");
        PLATFORM_NAMES.put("mp-weixin", "微信小程序");
        PLATFORM_NAMES.put("mp-kuaishou", "快手小程序");
        PLATFORM_NAMES.put("mp-baidu", "百度小程序");
    }

    @GetMapping("/list")
    @Operation(summary = "获取已构建的小程序列表", description = "获取dist/build目录下所有已构建的小程序信息")
    @OperationLog(opType = OpType.QUERY_CODE, description = "获取已构建的小程序列表")
    public Result<List<NovelAppBuildInfoDTO>> listBuildedApps() {
        try {
            String buildedPath = novelAppBuildUtil.getBuildedPath();
            File buildedDir = new File(buildedPath, "build");
            
            if (!buildedDir.exists() || !buildedDir.isDirectory()) {
                return Result.success("没有找到已构建的小程序", Collections.emptyList());
            }

            List<NovelAppBuildInfoDTO> buildInfoList = Arrays.stream(buildedDir.listFiles())
                .filter(File::isDirectory)
                .map(buildCodeDir -> {
                    NovelAppBuildInfoDTO buildInfo = new NovelAppBuildInfoDTO();
                    // 获取任意一个平台下的 project.config.json 中的 projectName
                    File[] platformDirs = buildCodeDir.listFiles();
                    String appName = "UnknownApp"; // 默认名称
                    if (platformDirs != null) {
                        for (File platformDir : platformDirs) {
                            if (PLATFORM_NAMES.containsKey(platformDir.getName())) {
                                File configFile = new File(platformDir, "project.config.json");
                                if (configFile.exists()) {
                                    try {
                                        JsonNode configJson = new ObjectMapper().readTree(configFile);
                                        appName = configJson.get("projectname").asText();
                                        break;
                                    } catch (Exception e) {
                                        appName = "InvalidConfig";
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    buildInfo.setAppName(appName);
                    
                    List<NovelAppBuildInfoDTO.PlatformInfo> platforms = Arrays.stream(buildCodeDir.listFiles())
                        .filter(File::isDirectory)
                        .filter(platformDir -> PLATFORM_NAMES.containsKey(platformDir.getName()))
                        .map(platformDir -> {
                            NovelAppBuildInfoDTO.PlatformInfo platformInfo = new NovelAppBuildInfoDTO.PlatformInfo();
                            platformInfo.setPlatformCode(platformDir.getName());
                            platformInfo.setPlatformName(PLATFORM_NAMES.get(platformDir.getName()));
                            
                            // 设置项目路径
                            platformInfo.setProjectPath(platformDir.getAbsolutePath());
                            
                            // 从 project.config.json 获取 appId
                            File configFile = new File(platformDir, "project.config.json");
                            if (configFile.exists()) {
                                try {
                                    JsonNode configJson = new ObjectMapper().readTree(configFile);
                                    String appId = configJson.get("appid").asText();
                                    platformInfo.setAppId(appId);
                                    
                                    // 从 NovelAppService 获取版本号
                                    try {
                                        NovelApp novelApp = novelAppService.getByAppId(appId);
                                        if (novelApp != null) {
                                            platformInfo.setVersion(novelApp.getVersion());
                                        } else {
                                            platformInfo.setVersion("未知版本");
                                        }
                                    } catch (Exception e) {
                                        platformInfo.setVersion("获取版本失败");
                                        e.printStackTrace();
                                    }
                                } catch (Exception e) {
                                    platformInfo.setAppId("InvalidAppId");
                                    platformInfo.setVersion("未知版本");
                                    e.printStackTrace();
                                }
                            } else {
                                platformInfo.setAppId("ConfigNotFound");
                                platformInfo.setVersion("未知版本");
                            }
                            
                            // 获取构建时间（使用目录的最后修改时间）
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                            platformInfo.setBuildTime(sdf.format(new Date(platformDir.lastModified())));
                            
                            return platformInfo;
                        })
                        .collect(Collectors.toList());
                    
                    buildInfo.setPlatforms(platforms);
                    return buildInfo;
                })
                .collect(Collectors.toList());

            return Result.success("获取成功", buildInfoList);
        } catch (Exception e) {
            return Result.error("获取小程序列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/publish")
    @Operation(summary = "发布小程序", description = "发布小程序到指定平台")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    @OperationLog(opType = OpType.OTHER_CODE, description = "发布小程序")
    public Result<NovelAppPublishDTO> publishNovelApp(@RequestBody Map<String, String> params) {
        try {
            String platformCode = params.get("platformCode");
            String appId = params.get("appId");
            String projectPath = params.get("projectPath");
            String douyinAppToken = params.get("douyinAppToken");
            String kuaishouAppToken = params.get("kuaishouAppToken");
            String weixinAppToken = params.get("weixinAppToken");
            String version = params.get("version");
            String log = params.get("log");

            // 验证必填参数
            if (platformCode == null || appId == null || projectPath == null || version == null || log == null) {
                return Result.error("缺少必要参数");
            }

            // 验证平台代码
            if (!PLATFORM_NAMES.containsKey(platformCode)) {
                return Result.error("不支持的平台代码: " + platformCode);
            }

            // 验证应用是否存在
            NovelApp novelApp = novelAppService.getByAppId(appId);
            if (novelApp == null) {
                return Result.error("未找到对应的应用: " + appId);
            }

            // 如果是抖音平台，验证token
            if ("mp-toutiao".equals(platformCode) && (douyinAppToken == null || douyinAppToken.trim().isEmpty())) {
                return Result.error("抖音平台发布需要提供 douyinAppToken");
            }

            // 如果是快手平台，验证token
            if ("mp-kuaishou".equals(platformCode) && (kuaishouAppToken == null || kuaishouAppToken.trim().isEmpty())) {
                return Result.error("快手平台发布需要提供 kuaishouAppToken");
            }

            // 如果是微信平台，验证token
            if ("mp-weixin".equals(platformCode) && (weixinAppToken == null || weixinAppToken.trim().isEmpty())) {
                return Result.error("微信平台发布需要提供 weixinAppToken");
            }

            // 创建发布任务
            String taskId = novelAppPublishUtil.publishNovelApp(
                platformCode,
                appId,
                projectPath,
                douyinAppToken,
                kuaishouAppToken, weixinAppToken,
                version,
                log
            );

            if (taskId == null) {
                return Result.error("当前平台已有发布任务在进行中，请等待完成后再试");
            }

            return Result.success("发布任务已启动", new NovelAppPublishDTO(taskId));
        } catch (Exception e) {
            logger.error("发布小程序失败", e);
            return Result.error("发布失败: " + e.getMessage());
        }
    }

    @PostMapping("/stop/{taskId}")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    public Result<String> stopPublish(@PathVariable String taskId) {
        try {
            novelAppPublishUtil.stopPublish(taskId);
            return Result.success("停止发布任务成功", null);
        } catch (Exception e) {
            logger.error("停止发布任务失败", e);
            return Result.error("停止发布失败: " + e.getMessage());
        }
    }

    @GetMapping("/qrcode/{taskId}")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    @Operation(summary = "获取发布任务的二维码", description = "获取指定发布任务生成的二维码图片")
    public ResponseEntity<byte[]> getQrcodeImage(@PathVariable String taskId) {
        try {
            logger.info("开始获取二维码图片，任务ID: {}", taskId);
            
            // 获取任务信息
            String platformCode = publishTaskManager.getPlatformCode(taskId);
            logger.info("任务平台代码: {}", platformCode);
            
            if (platformCode == null) {
                logger.warn("任务不存在: {}", taskId);
                return ResponseEntity.notFound().build();
            }
            
            if (!"mp-kuaishou".equals(platformCode)) {
                logger.warn("不是快手平台任务: {}", platformCode);
                return ResponseEntity.notFound().build();
            }

            // 获取项目路径
            String projectPath = publishTaskManager.getProjectPath(taskId);
            logger.info("项目路径: {}", projectPath);
            
            if (projectPath == null) {
                logger.warn("未找到项目路径");
                return ResponseEntity.notFound().build();
            }

            // 构建二维码文件路径
            String qrcodePath = projectPath + "\\ks_qrcode.png";
            logger.info("二维码文件路径: {}", qrcodePath);

            // 读取二维码文件
            File qrcodeFile = new File(qrcodePath);
            if (!qrcodeFile.exists()) {
                logger.warn("二维码文件不存在: {}", qrcodePath);
                return ResponseEntity.notFound().build();
            }

            // 读取文件内容
            byte[] imageBytes = java.nio.file.Files.readAllBytes(qrcodeFile.toPath());
            logger.info("成功读取二维码文件，大小: {} bytes", imageBytes.length);

            // 返回图片
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            logger.error("获取二维码图片失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 
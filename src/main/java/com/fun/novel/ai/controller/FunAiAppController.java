package com.fun.novel.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fun.novel.ai.entity.FunAiApp;
import com.fun.novel.service.FunAiAppService;
import com.fun.novel.service.FunAiUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * AI应用控制器
 */
@RestController
@RequestMapping("/api/fun-ai/apps")
@Tag(name = "Fun AI 应用管理", description = "AI应用的增删改查接口")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://172.17.5.80:5173",
        "http://172.17.5.80:8080"
}, allowCredentials = "true")
public class FunAiAppController {

    private static final Logger logger = LoggerFactory.getLogger(FunAiAppController.class);

    @Autowired
    private FunAiAppService funAiAppService;

    @Autowired
    private FunAiUserService funAiUserService;

    @Value("${funai.userPath}")
    private String userPath;

    /**
     * 获取处理后的用户路径（去除引号和空格）
     */
    private String getUserPath() {
        if (userPath == null) {
            return null;
        }
        // 去除首尾的引号和空格
        return userPath.trim().replaceAll("^[\"']|[\"']$", "");
    }


    /**
     * 创建应用
     * @param userId 用户ID
     * @return 创建后的应用信息
     */
    @Operation(summary = "创建应用", description = "创建新的AI应用")
    @GetMapping(path="/create")
    public ResponseEntity<FunAiApp> createApp(@Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        try {
            // 1. 创建用户专属文件夹（如果不存在）
            String basePath = getUserPath();
            if (basePath == null || basePath.isEmpty()) {
                logger.error("用户路径配置为空");
                return ResponseEntity.internalServerError().build();
            }
            String userDirPath = basePath + File.separator + userId;
            Path userDir = Paths.get(userDirPath);
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
                logger.info("创建用户专属文件夹: {}", userDirPath);
            }

            // 2. 创建FunAiApp对象，设置默认值
            FunAiApp app = new FunAiApp();
            app.setUserId(userId);
            app.setAppName("未命名");
            app.setAppStatus(1); // 默认启用

            // 3. 保存到数据库（会自动生成id）
            FunAiApp createdApp = funAiAppService.createApp(app);

            // 4. 使用生成的id创建应用文件夹
            try {
                String appDirName = "app_" + createdApp.getId();
                String appDirPath = userDirPath + File.separator + appDirName;
                Path appDir = Paths.get(appDirPath);
                Files.createDirectories(appDir);
                logger.info("创建应用文件夹: {}", appDirPath);
            } catch (Exception folderException) {
                // 如果创建文件夹失败，回滚数据库操作
                logger.error("创建应用文件夹失败，回滚数据库记录: appId={}, error={}", 
                    createdApp.getId(), folderException.getMessage(), folderException);
                try {
                    funAiAppService.removeById(createdApp.getId());
                    logger.info("已删除数据库记录: appId={}", createdApp.getId());
                } catch (Exception rollbackException) {
                    logger.error("回滚数据库记录失败: appId={}, error={}", 
                        createdApp.getId(), rollbackException.getMessage(), rollbackException);
                }
                throw new RuntimeException("创建应用文件夹失败: " + folderException.getMessage(), folderException);
            }

            return ResponseEntity.ok(createdApp);
        } catch (Exception e) {
            logger.error("创建应用失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取当前用户的应用列表
     * @return 应用列表
     */
    @GetMapping
    @Operation(summary = "获取应用列表", description = "获取当前用户的所有应用")
    public ResponseEntity<List<FunAiApp>> getApps() {
        // List<FunAiApp> apps = funAiAppService.getAppsByUserId(userId);
        // return ResponseEntity.ok(apps);
        return null;
    }

    /**
     * 分页获取当前用户的应用列表
     * @param page 页码
     * @param size 每页条数
     * @return 分页应用列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页获取应用列表", description = "分页获取当前用户的应用列表")
    public ResponseEntity<Page<FunAiApp>> getApps( 
            @Parameter(description = "页码，默认为1") 
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "每页条数，默认为10") 
            @RequestParam(defaultValue = "10") Integer size) {
        // Page<FunAiApp> apps = funAiAppService.getAppsByUserId(userId, page, size);
        // return ResponseEntity.ok(apps);
        return null;
    }

    /**
     * 获取单个应用信息
     * @param appId 应用ID
     * @return 应用信息
     */
    @GetMapping("/{appId}")
    @Operation(summary = "获取应用详情", description = "根据应用ID获取应用详情")
    public ResponseEntity<FunAiApp> getApp(@PathVariable Long appId) {
        // FunAiApp app = funAiAppService.getAppByIdAndUserId(appId, userId);
        // if (app == null) {
        //     return ResponseEntity.notFound().build();
        // }
        // return ResponseEntity.ok(app);
        return null;
    }

    /**
     * 更新应用信息
     * @param appId 应用ID
     * @param app 应用信息
     * @return 更新后的应用信息
     */
    @PutMapping("/{appId}")
    @Operation(summary = "更新应用", description = "更新应用信息")
    public ResponseEntity<FunAiApp> updateApp(@PathVariable Long appId, @RequestBody FunAiApp app) {
        // FunAiApp updatedApp = funAiAppService.updateApp(app, userId);
        // return ResponseEntity.ok(updatedApp);
        return null;
    }

    /**
     * 删除应用
     * @param appId 应用ID
     * @return 删除结果
     */
    @DeleteMapping("/{appId}")
    @Operation(summary = "删除应用", description = "根据应用ID删除应用")
    public ResponseEntity<Void> deleteApp(@PathVariable Long appId) {
        // boolean deleted = funAiAppService.deleteApp(appId, userId);
        // if (!deleted) {
        //     return ResponseEntity.notFound().build();
        // }
        // return ResponseEntity.ok().build();
        return null;
    }
}

package com.fun.novel.controller;

import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.entity.AppUIConfig;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.*;
import com.fun.novel.entity.AppCommonConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/novel-apps")
@Tag(name = "小说应用管理", description = "小说应用管理相关接口")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class NovelAppController {

    @Autowired
    private NovelAppService novelAppService;

    @Autowired
    private NovelAppLocalFileOperationService novelAppLocalFileOperationService;

    @Autowired
    private NovelAppResourceFileService novelAppResourceFileService;

    @Autowired
    private AppCommonConfigService appCommonConfigService;

    @Autowired
    private AppUIConfigService appUIConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建小说应用", description = "创建一个新的小说应用记录")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    @OperationLog(opType = OpType.INSERT_CODE, opName = "创建小说应用")
    public Result<NovelApp> addNovelApp(@Valid @RequestBody NovelApp novelApp) {
        NovelApp createdApp = novelAppService.addNovelApp(novelApp);
        return Result.success("应用创建成功", createdApp);
    }

    @GetMapping("/appLists")
    @Operation(summary = "获取分组小说应用列表", description = "获取按平台分组的小说应用列表")
    public Result<Map<String, List<NovelApp>>> getNovelAppsByPlatform() {
        Map<String, List<NovelApp>> groupedApps = novelAppService.getNovelAppsByPlatform();
        return Result.success("获取成功", groupedApps);
    }


    @GetMapping("/getByAppId")
    @Operation(summary = "根据应用ID获取小说应用", description = "根据应用ID获取小说应用的详细信息")
    @OperationLog(opType = OpType.QUERY_CODE, opName = "根据应用ID获取小说应用")
    public Result<NovelApp> getNovelAppByAppId(
            @Parameter(description = "应用ID", required = true)
            @RequestParam String appId) {
        try {
            NovelApp novelApp = novelAppService.getByAppId(appId);
            return novelApp != null ? 
                Result.success("获取成功", novelApp) : 
                Result.error("未找到对应的应用");
        } catch (Exception e) {
            return Result.error("获取应用失败: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    @Operation(summary = "修改小说应用基础信息", description = "根据传入的小说应用信息修改记录")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1','ROLE_2')")
    @OperationLog(opType = OpType.UPDATE_CODE, opName = "修改小说应用基础信息")
    public Result<NovelApp> updateNovelApp(@Valid @RequestBody NovelApp novelApp) {
        // 版本号校验逻辑
        NovelApp existingApp = novelAppService.getByAppId(novelApp.getAppid());
        if (existingApp != null) {
            String existingVersion = existingApp.getVersion();
            String newVersion = novelApp.getVersion();
            
            // 比较版本号
            if (compareVersion(newVersion, existingVersion) < 0) {
                return Result.error("当前版本不能小于最新版本号");
            }
        }
        
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();
        try {
            // 1. 数据库操作
            NovelApp updatedApp = novelAppService.updateNovelApp(novelApp);
            // 2. 文件操作
            CreateNovelAppRequest params = convertToCreateNovelAppRequest(novelApp);
            
            //更新appConfig,更新deliverConfig,更新主题色,更新pages-xx.json文件
            novelAppLocalFileOperationService.updateBaseConfigLocalCodeFiles(params, rollbackActions);
            //根据主题色处理资源文件
            novelAppResourceFileService.createResourceFilesSimple(params, rollbackActions);
            return Result.success("应用修改成功", updatedApp);
        } catch (Exception e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error("应用修改失败: " + e.getMessage());
        }
    }

    // NovelApp -> CreateNovelAppRequest 转换
    private CreateNovelAppRequest convertToCreateNovelAppRequest(NovelApp novelApp) {
        CreateNovelAppRequest req = new CreateNovelAppRequest();
        CreateNovelAppRequest.BaseConfig baseConfig = new CreateNovelAppRequest.BaseConfig();
        baseConfig.setAppName(novelApp.getAppName());
        baseConfig.setAppCode(novelApp.getAppCode());
        baseConfig.setPlatform(novelApp.getPlatform());
        baseConfig.setVersion(novelApp.getVersion());
        baseConfig.setProduct(novelApp.getProduct());
        baseConfig.setCustomer(novelApp.getCustomer());
        baseConfig.setAppid(novelApp.getAppid());
        baseConfig.setTokenId(novelApp.getTokenId());
        baseConfig.setCl(novelApp.getCl());
        baseConfig.setDeliverId(novelApp.getDeliverId());
        baseConfig.setBannerId(novelApp.getBannerId());
        req.setBaseConfig(baseConfig);

        CreateNovelAppRequest.CommonConfig commonConfig = new CreateNovelAppRequest.CommonConfig();
        // buildCode从通用配置查库获取
        AppCommonConfig dbCommonConfig = appCommonConfigService.getAppCommonConfig(novelApp.getAppid());
        if (dbCommonConfig != null) {
            commonConfig.setBuildCode(dbCommonConfig.getBuildCode());
            commonConfig.setContact(dbCommonConfig.getContact());
            commonConfig.setDouyinImId(dbCommonConfig.getDouyinImId());
            commonConfig.setKuaishouAppToken(dbCommonConfig.getKuaishouAppToken());
            commonConfig.setKuaishouClientId(dbCommonConfig.getKuaishouClientId());
            commonConfig.setKuaishouClientSecret(dbCommonConfig.getKuaishouClientSecret());
            commonConfig.setMineLoginType(dbCommonConfig.getMineLoginType());
            commonConfig.setReaderLoginType(dbCommonConfig.getReaderLoginType());
            commonConfig.setWeixinAppToken(dbCommonConfig.getWeixinAppToken());
            commonConfig.setDouyinAppToken(dbCommonConfig.getDouyinAppToken());
            commonConfig.setIaaMode(dbCommonConfig.getIaaMode());
            commonConfig.setIaaDialogStyle(dbCommonConfig.getIaaDialogStyle());

        }
        req.setCommonConfig(commonConfig);

        CreateNovelAppRequest.UiConfig uiConfig = new CreateNovelAppRequest.UiConfig();

        AppUIConfig dbUiConfig = appUIConfigService.getByAppId(novelApp.getAppid());
        if (dbUiConfig != null) {
            uiConfig.setHomeCardStyle(dbUiConfig.getHomeCardStyle());
            uiConfig.setPayCardStyle(dbUiConfig.getPayCardStyle());
            uiConfig.setMainTheme(dbUiConfig.getMainTheme());
            uiConfig.setSecondTheme(dbUiConfig.getSecondTheme());
        }
        req.setUiConfig(uiConfig);

        // 其它配置如有需要可补充
        return req;
    }

    @GetMapping("/delete")
    @Operation(summary = "删除小说应用", description = "根据应用ID删除小说应用")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    @OperationLog(opType = OpType.DELETE_CODE, opName = "删除小说应用")
    public Result<String> deleteNovelApp(
            @Parameter(description = "应用ID", required = true)
            @RequestParam String appId) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();

        try {
            NovelApp novelApp = novelAppService.getByAppId(appId);
            // 先转换数据，再删除数据库记录
            CreateNovelAppRequest params = convertToCreateNovelAppRequest(novelApp);
            List<NovelApp> appsByAppName = novelAppService.getAppsByAppName(novelApp.getAppName());

            boolean isLast = appsByAppName.size() == 1;
            // 删除代码文件
            novelAppLocalFileOperationService.deleteAppLocalCodeFiles(params,rollbackActions,isLast);
            //删除资源文件
            novelAppResourceFileService.deleteResourceFiles(params,rollbackActions,isLast);
            // 最后删除数据库记录
            boolean success = novelAppService.deleteByAppId(appId);
            
            return success ? Result.success("应用删除成功") 
                         : Result.error("应用删除失败");
        } catch (IllegalArgumentException e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error("应用删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 比较两个版本号
     * @param version1 新版本号
     * @param version2 旧版本号
     * @return 如果version1 > version2返回1，相等返回0，小于返回-1
     */
    private int compareVersion(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return 0; // 如果任一版本号为空，则认为相等
        }
        
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        int length = Math.max(v1Parts.length, v2Parts.length);
        
        for (int i = 0; i < length; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            
            if (v1Part > v2Part) {
                return 1;
            } else if (v1Part < v2Part) {
                return -1;
            }
        }
        
        return 0;
    }
}
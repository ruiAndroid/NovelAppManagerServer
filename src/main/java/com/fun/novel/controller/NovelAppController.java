package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppService;
import com.fun.novel.service.NovelAppLocalFileOperationService;
import com.fun.novel.service.NovelAppResourceFileService;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.service.AppCommonConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
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

    

    @PostMapping("/create")
    @Operation(summary = "创建小说应用", description = "创建一个新的小说应用记录")
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
    @Operation(summary = "修改小说应用", description = "根据传入的小说应用信息修改记录")
    public Result<NovelApp> updateNovelApp(@Valid @RequestBody NovelApp novelApp) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();
        try {
            // 1. 数据库操作
            NovelApp updatedApp = novelAppService.updateNovelApp(novelApp);
            // 2. 文件操作
            CreateNovelAppRequest params = convertToCreateNovelAppRequest(novelApp);
            
            //更新appConfig,更新deliverConfig,更新主题色,更新pages-xx.json文件
            novelAppLocalFileOperationService.updateBaseConfigLocalCodeFiles(params, rollbackActions);
            //根据主题色处理资源文件
            novelAppResourceFileService.processAllResourceFilesSimple(params, rollbackActions);
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
        baseConfig.setMainTheme(novelApp.getMainTheme());
        baseConfig.setSecondTheme(novelApp.getSecondTheme());
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
            commonConfig.setPayCardStyle(dbCommonConfig.getPayCardStyle());
            commonConfig.setHomeCardStyle(dbCommonConfig.getHomeCardStyle());
            commonConfig.setReaderLoginType(dbCommonConfig.getReaderLoginType());
            commonConfig.setWeixinAppToken(dbCommonConfig.getWeixinAppToken());
            commonConfig.setDouyinAppToken(dbCommonConfig.getDouyinAppToken());
            commonConfig.setIaaMode(dbCommonConfig.getIaaMode());
        }
        req.setCommonConfig(commonConfig);

        CreateNovelAppRequest.DeliverConfig deliverConfig = new CreateNovelAppRequest.DeliverConfig();
        deliverConfig.setDeliverId(novelApp.getDeliverId());
        deliverConfig.setBannerId(novelApp.getBannerId());
        req.setDeliverConfig(deliverConfig);
        // 其它配置如有需要可补充
        return req;
    }

    @GetMapping("/delete")
    @Operation(summary = "删除小说应用", description = "根据应用ID删除小说应用")
    public Result<String> deleteNovelApp(
            @Parameter(description = "应用ID", required = true)
            @RequestParam String appId) {
        try {
            boolean success = novelAppService.deleteByAppId(appId);
            return success ? Result.success("应用删除成功") 
                         : Result.error("应用删除失败");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
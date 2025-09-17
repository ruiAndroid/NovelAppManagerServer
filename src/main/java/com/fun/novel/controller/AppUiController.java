package com.fun.novel.controller;

import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.entity.AppWeijuBanner;
import com.fun.novel.entity.AppUIConfig;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.AppCommonConfigService;
import com.fun.novel.service.AppUIConfigService;
import com.fun.novel.service.NovelAppService;
import com.fun.novel.service.fileOpeartionService.UiConfigFileOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/novel-ui")
@Tag(name = "小说ui配置管理", description = "小说ui配置管理接口")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AppUiController {

    @Autowired
    private AppUIConfigService appUIConfigService;

    @Autowired
    private UiConfigFileOperationService uiConfigFileOperationService;

    @Autowired
    private NovelAppService novelAppService;

    @Autowired
    private AppCommonConfigService appCommonConfigService;

    @PostMapping("/createUiConfig")
    @Operation(summary = "创建Ui配置", description = "创建新的Ui配置记录")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1','ROLE_2')")
    @OperationLog(opType = OpType.INSERT_CODE, description = "创建Ui配置")
    public Result<AppUIConfig> createAppUiConfig(
            @Parameter(description = "appUIConfig对象", required = true)
            @Valid @RequestBody AppUIConfig appUIConfig) {
        try {
            AppUIConfig createdConfig = appUIConfigService.createAppUIConfig(appUIConfig);
            return Result.success("创建成功", createdConfig);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/updateUiConfig")
    @Operation(summary = "更新Ui配置", description = "更新Ui配置记录")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1','ROLE_2')")
    @OperationLog(opType = OpType.UPDATE_CODE, description = "更新Ui配置")
    public Result<AppUIConfig> updateAppUiConfig(
            @Parameter(description = "appUIConfig对象", required = true)
            @Valid @RequestBody AppUIConfig appUIConfig) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();

        try {
            //1.数据库操作
            AppUIConfig updatedConfig = appUIConfigService.updateAppUIConfig(appUIConfig);
            //通过config的appId 反向查询novelApp
            NovelApp novelApp = novelAppService.getByAppId(updatedConfig.getAppid());


            CreateNovelAppRequest params = convertToCreateNovelAppRequest(novelApp);
            //2. uiConfig文件操作
            uiConfigFileOperationService.updateUiConfigLocalCodeFiles(params,rollbackActions);

            return Result.success("更新成功", updatedConfig);
        } catch (IllegalArgumentException e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error("更新失败: " + e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/deleteUiConfig")
    @Operation(summary = "删除Ui配置", description = "根据appid删除Ui配置记录")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1','ROLE_2')")
    @OperationLog(opType = OpType.DELETE_CODE, description = "删除Ui配置")
    public Result<String> deleteAppUiConfig(
            @Parameter(description = "应用ID", required = true)
            @RequestParam String appid) {
        try {
            boolean deleted = appUIConfigService.deleteAppUIConfigByAppId(appid);
            if (deleted) {
                return Result.success("删除成功");
            } else {
                return Result.error(500, "删除失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/getUiConfig")
    @Operation(summary = "获取Ui配置", description = "根据appid获取Ui配置记录")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1','ROLE_2')")
    public Result<AppUIConfig> getAppUiConfig(
            @Parameter(description = "应用ID", required = true)
            @RequestParam String appId) {
        try {
            AppUIConfig config = appUIConfigService.getByAppId(appId);
            if (config != null) {
                return Result.success("查询成功", config);
            } else {
                return Result.error(404, "未找到对应配置");
            }
        } catch (Exception e) {
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

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
            commonConfig.setIaaDialogStyle(dbCommonConfig.getIaaDialogStyle());
        }
        req.setCommonConfig(commonConfig);

        CreateNovelAppRequest.DeliverConfig deliverConfig = new CreateNovelAppRequest.DeliverConfig();
        deliverConfig.setDeliverId(novelApp.getDeliverId());
        deliverConfig.setBannerId(novelApp.getBannerId());
        req.setDeliverConfig(deliverConfig);
        // 其它配置如有需要可补充
        return req;
    }

}
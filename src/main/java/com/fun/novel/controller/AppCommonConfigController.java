package com.fun.novel.controller;

import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.AppCommonConfigService;
import com.fun.novel.service.NovelAppLocalFileOperationService;
import com.fun.novel.service.NovelAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "应用通用配置接口")
@RestController
@RequestMapping("/api/novel-common")
@RequiredArgsConstructor
public class AppCommonConfigController {

    private final AppCommonConfigService appCommonConfigService;


    @Autowired
    private NovelAppLocalFileOperationService novelAppLocalFileOperationService;

    @Autowired
    private NovelAppService novelAppService;

    @PostMapping("/createAppCommonConfig")
    @Operation(summary = "创建应用通用配置", description = "创建新的应用通用配置")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    @OperationLog(opType = OpType.INSERT_CODE, opName = "创建新的应用通用配置")
    public Result<AppCommonConfig> createAppCommonConfig(
            @Parameter(description = "应用通用配置信息", required = true)
            @Valid @RequestBody AppCommonConfigDTO dto) {

        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();

        try {
            AppCommonConfig config = appCommonConfigService.createAppCommonConfig(dto);
            NovelApp novelApp = novelAppService.getByAppId(config.getAppId());

            CreateNovelAppRequest params = convertToCreateNovelAppRequest(novelApp);

            novelAppLocalFileOperationService.updateCommonConfigLocalCodeFiles(params, rollbackActions);

            return Result.success("创建成功", config);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/getAppCommonConfig")
    @Operation(summary = "获取应用通用配置", description = "根据appid获取应用通用配置信息")
    @OperationLog(opType = OpType.QUERY_CODE, opName = "根据appid获取应用通用配置信息")
    public Result<AppCommonConfig> getAppCommonConfig(
            @Parameter(description = "小程序ID", required = true)
            @RequestParam String appId) {
        AppCommonConfig config = appCommonConfigService.getAppCommonConfig(appId);
        if (config == null) {
            return Result.success("未找到对应的应用配置",config);
        }
        return Result.success("获取成功", config);
    }
    
    @GetMapping("/getAppCommonConfigByAppName")
    @Operation(summary = "根据应用名称获取通用配置", description = "根据应用名称获取所有平台的通用配置信息")
    @OperationLog(opType = OpType.QUERY_CODE, opName = "根据应用名称获取所有平台的通用配置信息")
    public Result<List<AppCommonConfig>> getAppCommonConfigByAppName(
            @Parameter(description = "应用名称", required = true)
            @RequestParam String appName) {
        List<AppCommonConfig> configs = appCommonConfigService.getAppCommonConfigByAppName(appName);
        if (configs == null || configs.isEmpty()) {
            return Result.success("未找到对应的应用配置",configs);
        }
        return Result.success("获取成功", configs);
    }

    @GetMapping("/deleteAppCommonConfig")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    @Operation(summary = "删除应用通用配置", description = "根据appId删除应用通用配置")
    @OperationLog(opType = OpType.DELETE_CODE, opName = "根据appId删除应用通用配置")
    public Result<String> deleteAppCommonConfig(
            @Parameter(description = "小程序ID", required = true)
            @RequestParam String appId) {
        try {
            boolean success = appCommonConfigService.deleteAppCommonConfig(appId);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("未找到对应的应用配置");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/updateAppCommonConfig")
    @Operation(summary = "更新应用通用配置", description = "更新应用通用配置信息")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    @OperationLog(opType = OpType.UPDATE_CODE, opName = "更新应用通用配置信息")
    public Result<AppCommonConfig> updateAppCommonConfig(
            @Parameter(description = "应用通用配置信息", required = true)
            @Valid @RequestBody AppCommonConfigDTO dto) {
        java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();
        try {
            // 1. 数据库操作
            AppCommonConfig config = appCommonConfigService.updateAppCommonConfig(dto);

            //通过config的appId 反向查询novelApp
            NovelApp novelApp = novelAppService.getByAppId(config.getAppId());


            CreateNovelAppRequest params = convertToCreateNovelAppRequest(novelApp);

            // 2. commonConfig文件操作
            novelAppLocalFileOperationService.updateCommonConfigLocalCodeFiles(params, rollbackActions);


            return Result.success("更新成功", config);
        } catch (Exception e) {
            // 回滚所有文件操作
            for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
            }
            return Result.error("更新失败: " + e.getMessage());
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
        // 其它配置如有需要可补充
        return req;
    }


}
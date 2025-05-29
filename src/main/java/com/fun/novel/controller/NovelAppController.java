package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.service.NovelAppService;
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

    @PostMapping("/create")
    @Operation(summary = "新增小说应用", description = "创建一个新的小说应用记录")
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
        NovelApp updatedApp = novelAppService.updateNovelApp(novelApp);
        return Result.success("应用修改成功", updatedApp);
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
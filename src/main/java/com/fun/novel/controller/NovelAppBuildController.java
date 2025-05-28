package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.utils.NovelAppBuildUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/novel-build")
@Tag(name = "UniApp构建", description = "UniApp项目构建相关接口")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class NovelAppBuildController {

    @Autowired
    private NovelAppBuildUtil novelAppBuildUtil;

    @PostMapping("/build")
    @Operation(summary = "构建UniApp项目", description = "执行npm run dev命令构建UniApp项目")
    public Result<String> buildNovelApp(
            @Parameter(description = "项目路径", required = true)
            @RequestParam String cmd) {
        try {
            novelAppBuildUtil.buildNovelApp(cmd);
            return Result.success("构建任务已启动，请通过WebSocket监听构建日志");
        } catch (Exception e) {
            return Result.error("启动构建任务失败: " + e.getMessage());
        }
    }
} 
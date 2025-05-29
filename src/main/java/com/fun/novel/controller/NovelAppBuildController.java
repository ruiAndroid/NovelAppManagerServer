package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.utils.NovelAppBuildUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/novel-build")
@Tag(name = "UniApp构建", description = "UniApp项目构建相关接口")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://172.17.5.80:5173",
    "http://172.17.5.80:8080"
}, allowCredentials = "true")
public class NovelAppBuildController {

    @Autowired
    private NovelAppBuildUtil novelAppBuildUtil;

    @PostMapping("/build")
    @Operation(summary = "构建UniApp项目", description = "异步执行构建命令，返回任务ID用于WebSocket订阅")
    public Result<String> buildNovelApp(
            @Parameter(description = "构建命令", required = true)
            @RequestParam String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return Result.error("构建命令不能为空");
        }
        try {
            String taskId = novelAppBuildUtil.buildNovelApp(cmd);
            return Result.success("构建任务已启动，请使用任务ID订阅WebSocket日志", taskId);
        } catch (Exception e) {
            return Result.error("启动构建任务失败: " + e.getMessage());
        }
    }

    @GetMapping("/stop")
    @Operation(summary = "停止构建", description = "停止指定任务的构建进程")
    public Result<String> stopBuild(
            @Parameter(description = "任务ID", required = true)
            @RequestParam(name = "taskId", required = true) String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return Result.error("任务ID不能为空");
        }
        try {
            novelAppBuildUtil.stopBuild(taskId);
            return Result.success("已发送停止命令");
        } catch (Exception e) {
            return Result.error("停止构建失败: " + e.getMessage());
        }
    }
} 
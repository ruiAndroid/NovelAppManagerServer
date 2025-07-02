package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.dto.CreateNovelLogType;
import com.fun.novel.service.NovelAppCreationService;
import com.fun.novel.utils.CreateNovelTaskManager;
import com.fun.novel.utils.CreateNovelTaskLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/novel-create")
@Tag(name = "小程序发布", description = "小程序创建相关接口")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://172.17.5.80:5173",
    "http://172.17.5.80:8080"
}, allowCredentials = "true")
public class NovelAppCreateController {

    @Autowired
    private CreateNovelTaskManager createNovelTaskManager;

    @Autowired
    private CreateNovelTaskLogger taskLogger;

    @Autowired
    private NovelAppCreationService novelAppCreationService;

    @Operation(summary = "创建小说小程序")
    @PostMapping("/createNovelApp")
    public Result<Map<String, String>> createNovelApp(@RequestBody CreateNovelAppRequest params) {
        String taskId = createNovelTaskManager.createTask();
        if (taskId == null) {
            return Result.error("已有小说创建任务正在进行中，请稍后再试");
        }
        // 立即返回taskId给前端
        Map<String, String> data = new HashMap<>();
        data.put("taskId", taskId);
        Result<Map<String, String>> result = Result.success("任务已启动", data);

        // 异步执行实际创建流程
        CompletableFuture.runAsync(() -> {
            String currentTaskId = taskId; // Make a final copy for the lambda

            // 用于非数据库操作的回滚
            List<Runnable> rollbackActions = new ArrayList<>();

            try {
                // 等待一段时间，给前端足够的时间来订阅
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    taskLogger.log(currentTaskId, "任务线程被中断，创建失败", CreateNovelLogType.ERROR);
                    return;
                }
                taskLogger.log(currentTaskId, "参数校验已通过，开始执行创建小说小程序任务 taskId:" + currentTaskId, CreateNovelLogType.INFO);

                //数据库+本地代码文件处理 (事务性)
                novelAppCreationService.processAllOperations(currentTaskId, params, rollbackActions);

            } catch (Exception e) {

                taskLogger.log(currentTaskId, "创建小说小程序任务失败: " + e.getMessage(), CreateNovelLogType.ERROR);
                 // 回滚所有非数据库操作
                for (int i = rollbackActions.size() - 1; i >= 0; i--) {
                    try { rollbackActions.get(i).run(); } catch (Exception ignore) {}
                }
            } finally {
                createNovelTaskManager.removeTask(currentTaskId);
                taskLogger.log(currentTaskId, "Create Novel Finish", CreateNovelLogType.FINISH);
            }
        });

        return result;
    }
}

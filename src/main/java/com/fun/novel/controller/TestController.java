package com.fun.novel.controller;

import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.enums.OpType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器，用于验证操作日志功能
 */
@RestController
@RequestMapping("/api/test")
public class TestController {




    @OperationLog(opType = OpType.QUERY_CODE, description = "查询测试数据")
    @GetMapping("/query")
    public Result<Map<String, Object>> queryTestData() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "查询成功");
        result.put("data", "test data");
        return Result.success(result);
    }
    
    @OperationLog(opType = OpType.INSERT_CODE, description = "新增测试数据")
    @PostMapping("/insert")
    public Result<Map<String, Object>> insertTestData(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "新增成功");
        result.put("id", 1);
        return Result.success(result);
    }
    
    @OperationLog(opType = OpType.UPDATE_CODE, description = "更新测试数据")
    @PostMapping("/update")
    public Result<Map<String, Object>> updateTestData(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "更新成功");
        return Result.success(result);
    }

}
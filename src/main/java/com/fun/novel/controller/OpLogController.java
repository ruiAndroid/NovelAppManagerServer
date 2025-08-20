package com.fun.novel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.dto.UserOpLogDTO;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.UserOpLogService;
import com.fun.novel.entity.UserOpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志功能控制器
 */
@RestController
@Tag(name = "操作日志接口", description = "操作日志相关接口")
@RequestMapping("/api/op-log")
public class OpLogController {

    @Autowired
    private UserOpLogService userOpLogService;

    @GetMapping("/queryUserAllOp")
    @Operation(summary = "获取指定用户的所有操作记录", description = "获取指定用户的所有操作记录")
    @Parameters({
        @Parameter(name = "userId", description = "用户ID"),
        @Parameter(name = "page", description = "页码，从1开始"),
        @Parameter(name = "size", description = "每页条数")
    })
    public Result<IPage<UserOpLogDTO>> queryUserAllOp(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        // 创建分页对象
        Page<UserOpLog> pageParam = new Page<>(page, size);
        
        // 根据用户ID分页查询所有操作日志，并按更新时间倒序排列
        IPage<UserOpLog> userOpLogPage = userOpLogService.queryUserAllOpWithPage(Long.valueOf(userId), pageParam);

        // 转换为DTO对象
        List<UserOpLogDTO> userOpLogDTOs = userOpLogPage.getRecords().stream().map(userOpLog -> UserOpLogDTO.builder()
                .id(userOpLog.getId())
                .userId(userOpLog.getUserId())
                .userName(userOpLog.getUserName())
                .opType(userOpLog.getOpType())
                .opStatus(userOpLog.getOpStatus())
                .methodName(userOpLog.getMethodName())
                .requestType(userOpLog.getRequestType())
                .requestUrl(userOpLog.getRequestUrl())
                .requestIp(userOpLog.getRequestIp())
                .requestParams(userOpLog.getRequestParams())
                .responseResult(userOpLog.getResponseResult())
                .updateTime(userOpLog.getUpdateTime())
                .build()).collect(Collectors.toList());

        // 构造返回的分页结果
        IPage<UserOpLogDTO> userOpLogDTOPage = new Page<>(userOpLogPage.getCurrent(), userOpLogPage.getSize(), userOpLogPage.getTotal());
        userOpLogDTOPage.setRecords(userOpLogDTOs);
        
        return Result.success(userOpLogDTOPage);
    }

    @GetMapping("/queryAllOp")
    @Operation(summary = "获取所有操作记录", description = "获取所有操作记录，默认按时间倒序")
    @Parameters({
        @Parameter(name = "page", description = "页码，从1开始"),
        @Parameter(name = "size", description = "每页条数")
    })
    public Result<IPage<UserOpLogDTO>> queryAllOp(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        // 创建分页对象
        Page<UserOpLog> pageParam = new Page<>(page, size);
        
        // 分页查询所有操作日志，并按更新时间倒序排列
        IPage<UserOpLog> userOpLogPage = userOpLogService.queryAllOpWithPage(pageParam);

        // 转换为DTO对象
        List<UserOpLogDTO> userOpLogDTOs = userOpLogPage.getRecords().stream().map(userOpLog -> UserOpLogDTO.builder()
                .id(userOpLog.getId())
                .userId(userOpLog.getUserId())
                .userName(userOpLog.getUserName())
                .opType(userOpLog.getOpType())
                .opStatus(userOpLog.getOpStatus())
                .methodName(userOpLog.getMethodName())
                .requestType(userOpLog.getRequestType())
                .requestUrl(userOpLog.getRequestUrl())
                .requestIp(userOpLog.getRequestIp())
                .requestParams(userOpLog.getRequestParams())
                .responseResult(userOpLog.getResponseResult())
                .updateTime(userOpLog.getUpdateTime())
                .build()).collect(Collectors.toList());

        // 构造返回的分页结果
        IPage<UserOpLogDTO> userOpLogDTOPage = new Page<>(userOpLogPage.getCurrent(), userOpLogPage.getSize(), userOpLogPage.getTotal());
        userOpLogDTOPage.setRecords(userOpLogDTOs);
        
        return Result.success(userOpLogDTOPage);
    }
}
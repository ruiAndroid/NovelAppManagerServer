package com.fun.novel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.dto.UserOpArchiveDTO;
import com.fun.novel.dto.UserOpLogDTO;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.UserOpLogService;
import com.fun.novel.entity.UserOpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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


    @GetMapping("/queryAllOpByQuery")
    @Operation(summary = "根据查询条件获取所有操作记录", description = "根据查询条件获取所有操作记录")
    @Parameters({
        @Parameter(name = "query", description = "查询条件"),
        @Parameter(name = "page", description = "页码，从1开始"),
        @Parameter(name = "size", description = "每页条数")
    })
    public Result<IPage<UserOpLogDTO>> queryAllOpByQuery(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            // 创建分页对象
            Page<UserOpLog> pageParam = new Page<>(page, size);
            
            // 根据查询条件分页查询所有操作日志
            IPage<UserOpLog> userOpLogPage = userOpLogService.queryAllOpWithPageAndQuery(query, pageParam);

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
        } catch (Exception e) {
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/queryUserArchive")
    @Operation(summary = "获取用户所有重要操作记录", description = "获取用户所有重要操作记录")
    @Parameters({
            @Parameter(name = "query", description = "查询关键字"),
            @Parameter(name = "startTime", description = "起始时间"),
            @Parameter(name = "endTime", description = "结束时间")
    })
    @PreAuthorize("hasAnyRole('ROLE_0')")
    public Result<List<UserOpArchiveDTO>> queryUserArchive( @RequestParam(required = false) String query,
                                                            @RequestParam(required = false) String startTime,
                                                            @RequestParam(required = false) String endTime){
        try {
            // 计算默认时间范围（最近一年）
            java.time.LocalDateTime end = (endTime == null || endTime.isEmpty()) ? 
                java.time.LocalDateTime.now() : parseDateTime(endTime);
            // 如果结束时间只指定了日期（没有具体时间），则将结束时间设为当天的最后一刻
            if (endTime != null && !endTime.isEmpty() && !endTime.contains(":")) {
                end = end.with(java.time.LocalTime.MAX); // 设置为23:59:59.999999999
            }
            
            java.time.LocalDateTime start = (startTime == null || startTime.isEmpty()) ? 
                end.minusYears(1) : parseDateTime(startTime);
            // 如果开始时间只指定了日期（没有具体时间），则将开始时间设为当天的开始时刻
            if (startTime != null && !startTime.isEmpty() && !startTime.contains(":")) {
                start = start.with(java.time.LocalTime.MIN); // 设置为00:00:00
            }
            
            // 定义重要操作类型
            java.util.Set<Integer> importantOpTypes = java.util.Set.of(2, 3, 4, 7);
            
            // 查询操作日志
            java.util.List<com.fun.novel.entity.UserOpLog> logs;
            
            // 根据查询条件过滤日志
            java.time.LocalDateTime finalStart = start;
            java.time.LocalDateTime finalEnd = end;
            logs = userOpLogService.queryAllOp()
                .stream()
                .filter(log -> {
                    java.time.LocalDateTime updateTime = log.getUpdateTime();
                    // 添加空值检查
                    if (updateTime == null) return false;
                    
                    // 时间范围检查：更新时间在[start, end]范围内（包含边界）
                    boolean timeInRange = (updateTime.isEqual(finalStart) || updateTime.isAfter(finalStart)) &&
                                         (updateTime.isEqual(finalEnd) || updateTime.isBefore(finalEnd));
                    
                    // 操作类型检查
                    boolean isImportantType = importantOpTypes.contains(log.getOpType());
                    
                    // 如果查询条件为空，则不过滤；否则根据查询条件过滤
                    boolean matchesQuery = true;
                    if (query != null && !query.isEmpty()) {
                        matchesQuery =
                                (log.getUserId() != null && query.equals(log.getUserId().toString())) ||
                                (log.getUserName() != null && log.getUserName().contains(query)) ||
                                      (log.getOpName() != null && log.getOpName().contains(query)) ||
                                      (log.getMethodName() != null && log.getMethodName().contains(query)) ||
                                      (log.getRequestUrl() != null && log.getRequestUrl().contains(query)) ||
                                      (log.getRequestIp() != null && log.getRequestIp().contains(query)) ||
                                      (log.getRequestParams() != null && log.getRequestParams().contains(query)) ||
                                      (log.getResponseResult() != null && log.getResponseResult().contains(query));
                    }
                    
                    return timeInRange && isImportantType && matchesQuery;
                })
                .collect(java.util.stream.Collectors.toList());
            
            // 转换为UserOpArchiveDTO并格式化时间
            java.util.List<UserOpArchiveDTO> result = logs.stream()
                .map(log -> UserOpArchiveDTO.builder()
                    .userId(log.getUserId())
                    .userName(log.getUserName())
                    .opType(log.getOpType())
                    .opStatus(log.getOpStatus())
                    .opName(log.getOpName())
                    .requestUrl(log.getRequestUrl())
                    .requestParams(log.getRequestParams())
                    .responseResult(log.getResponseResult())
                    .updateTime(formatDateTime(log.getUpdateTime()))
                    .build())
                .sorted((a, b) -> b.getUpdateTime().compareTo(a.getUpdateTime())) // 按时间倒序排序
                .collect(java.util.stream.Collectors.toList());
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析日期时间字符串，支持多种格式
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime对象
     */
    private java.time.LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return java.time.LocalDateTime.now();
        }
        
        try {
            // 首先尝试解析为完整的LocalDateTime
            try {
                // 尝试标准完整格式 yyyy-MM-dd HH:mm:ss
                return java.time.LocalDateTime.parse(dateTimeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {}
            
            try {
                // 尝试标准格式 yyyy-MM-dd HH:mm
                return java.time.LocalDateTime.parse(dateTimeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception ignored) {}
            
            try {
                // 尝试非零填充格式 yyyy-M-d HH:mm:ss
                return java.time.LocalDateTime.parse(dateTimeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss"));
            } catch (Exception ignored) {}
            
            try {
                // 尝试非零填充格式 yyyy-M-d HH:mm
                return java.time.LocalDateTime.parse(dateTimeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d HH:mm"));
            } catch (Exception ignored) {}
            
            // 如果以上都不匹配，则尝试解析为日期格式
            try {
                // 尝试标准日期格式 yyyy-MM-dd
                return java.time.LocalDate.parse(dateTimeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            } catch (Exception ignored) {}
            
            try {
                // 尝试非零填充日期格式 yyyy-M-d
                return java.time.LocalDate.parse(dateTimeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d")).atStartOfDay();
            } catch (Exception ignored) {}
            
        } catch (Exception ignored) {
            // 所有格式都失败，抛出异常
        }
        
        // 如果所有格式都失败，则抛出异常
        throw new IllegalArgumentException("无法解析日期时间字符串: " + dateTimeStr);
    }

    /**
     * 格式化日期时间为"xxxx年xx月xx日xx时xx分xx秒"格式
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return String.format("%04d年%02d月%02d日%02d时%02d分%02d秒",
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute(),
                dateTime.getSecond());
    }

}
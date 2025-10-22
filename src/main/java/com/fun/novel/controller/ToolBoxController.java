package com.fun.novel.controller;

import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Result;
import com.fun.novel.dto.AppUploadCheckDTO;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.AppUploadCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/novel-toolbox")
@Tag(name = "工具箱", description = "工具箱相关接口")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://172.17.5.80:5173",
        "http://172.17.5.80:8080"
}, allowCredentials = "true")

public class ToolBoxController {

    private static final Logger logger = LoggerFactory.getLogger(ToolBoxController.class);
    
    @Autowired
    private AppUploadCheckService appUploadCheckService;

    @GetMapping("/app-upload-check")
    @Operation(summary = "发版前小程序检查", description = "发版前小程序检查")
    @PreAuthorize("hasAnyRole('ROLE_0','ROLE_1')")
    @OperationLog(opType = OpType.OTHER_CODE, opName = "发版前小程序检查")
    @Parameters({@Parameter(name = "appId", description = "小程序appid")})
    public Result<AppUploadCheckDTO> appUploadCheck(@RequestParam String appId) {
        try {
            logger.info("开始执行发版前小程序检查，appId: {}", appId);
            
            AppUploadCheckDTO result = appUploadCheckService.performUploadCheck(appId);
            
            logger.info("发版前小程序检查完成，appId: {}", appId);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("发版前小程序检查异常，appId: {}", appId, e);
            return Result.error("检查过程中发生异常: " + e.getMessage());
        }
    }
}
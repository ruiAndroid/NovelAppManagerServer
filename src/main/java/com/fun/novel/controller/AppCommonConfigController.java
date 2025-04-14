package com.fun.novel.controller;

import com.fun.novel.dto.AppCommonConfigDTO;
import com.fun.novel.entity.AppCommonConfig;
import com.fun.novel.service.AppCommonConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "应用通用配置接口")
@RestController
@RequestMapping("/api/app/config")
@RequiredArgsConstructor
public class AppCommonConfigController {

    private final AppCommonConfigService appCommonConfigService;

    @PostMapping("/save")
    @Operation(summary = "保存或更新应用配置")
    public boolean saveOrUpdateConfig(@Valid @RequestBody AppCommonConfigDTO dto) {
        return appCommonConfigService.saveOrUpdateConfig(dto);
    }

    @GetMapping("/{appid}")
    @Operation(summary = "获取应用配置")
    public AppCommonConfig getConfig(@PathVariable String appid) {
        return appCommonConfigService.getConfig(appid);
    }
} 
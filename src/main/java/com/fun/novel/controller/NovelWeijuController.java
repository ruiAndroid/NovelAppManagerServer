package com.fun.novel.controller;

import com.fun.novel.common.Result;
import com.fun.novel.entity.NovelWeijuBanner;
import com.fun.novel.entity.NovelWeijuDeliver;
import com.fun.novel.service.NovelWeijuBannerService;
import com.fun.novel.service.NovelWeijuDeliverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/novel-weiju")
@Tag(name = "小说微距管理", description = "小说微距Banner和Deliver管理接口")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class NovelWeijuController {

    @Autowired
    private NovelWeijuBannerService bannerService;

    @Autowired
    private NovelWeijuDeliverService deliverService;

    // Banner相关接口
    @PostMapping("/banner/createBanner")
    @Operation(summary = "创建Banner", description = "创建新的Banner记录")
    public Result<NovelWeijuBanner> createBanner(
            @Parameter(description = "Banner对象", required = true)
            @Valid @RequestBody NovelWeijuBanner banner) {
        NovelWeijuBanner createdBanner = bannerService.addBanner(banner);
        return Result.success("创建成功", createdBanner);
    }

    @GetMapping("/banner/getBannerByBannerId")
    @Operation(summary = "根据bannerId获取Banner", description = "根据bannerId查询对应的Banner记录")
    public Result<NovelWeijuBanner> getBannerByBannerId(
            @Parameter(description = "Banner ID", required = true)
            @RequestParam String bannerId) {
        NovelWeijuBanner banner = bannerService.getBannerByBannerId(bannerId);
        if (banner == null) {
            return Result.error("未找到对应的Banner记录");
        }
        return Result.success("获取成功", banner);
    }

    @PostMapping("/banner/updateBanner")
    @Operation(summary = "更新Banner", description = "更新Banner信息")
    public Result<NovelWeijuBanner> updateBanner(
            @Parameter(description = "Banner对象", required = true)
            @Valid @RequestBody NovelWeijuBanner banner) {
        try {
            NovelWeijuBanner updatedBanner = bannerService.updateBanner(banner);
            return Result.success("更新成功", updatedBanner);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }


    @GetMapping("/banner/deleteBannerByBannerId")
    @Operation(summary = "根据bannerId删除Banner", description = "根据bannerId删除对应的Banner记录")
    public Result<String> deleteBannerByBannerId(
            @Parameter(description = "Banner ID", required = true)
            @RequestParam String bannerId) {
        boolean success = bannerService.deleteBannerByBannerId(bannerId);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败，记录不存在");
        }
    }

    @GetMapping("/deliver/getDeliverByDeliverId")
    @Operation(summary = "根据deliverId获取Deliver", description = "根据deliverId查询对应的Deliver记录")
    public Result<NovelWeijuDeliver> getDeliverByDeliverId(
            @Parameter(description = "Deliver ID", required = true)
            @RequestParam String deliverId) {
        NovelWeijuDeliver deliver = deliverService.getDeliverByDeliverId(deliverId);
        if (deliver == null) {
            return Result.error("未找到对应的Deliver记录");
        }
        return Result.success("获取成功", deliver);
    }

    @PostMapping("/deliver/createDeliver")
    @Operation(summary = "创建Deliver", description = "创建新的Deliver记录")
    public Result<NovelWeijuDeliver> createDeliver(
            @Parameter(description = "Deliver对象", required = true)
            @Valid @RequestBody NovelWeijuDeliver deliver) {
        NovelWeijuDeliver createdDeliver = deliverService.addDeliver(deliver);
        return Result.success("创建成功", createdDeliver);
    }

    @GetMapping("/deliver/deleteByDeliverId")
    @Operation(summary = "根据deliverId删除Deliver", description = "根据deliverId删除对应的Deliver记录")
    public Result<String> deleteDeliverByDeliverId(
            @Parameter(description = "Deliver ID", required = true)
            @RequestParam String deliverId) {
        boolean success = deliverService.deleteByDeliverId(deliverId);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败，记录不存在");
        }
    }

} 
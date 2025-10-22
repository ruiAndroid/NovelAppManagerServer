package com.fun.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "发版前小程序检查结果")
public class AppUploadCheckDTO {

    //是否有测试代码遗留
    private boolean hasTestCode;

    //版本号信息
    private VersionInfo versionInfo;

    //微距配置项
    private DeliverInfo deliverInfo;

    //支付配置项
    private AppPayWithConfigDTO appPayWithConfig;

    //广告配置项
    private AppAdWithConfigDTO appAdWithConfig;


    @Data
    @Schema(description = "版本号信息")
    public static class VersionInfo {
        //线上版本号
        private String onlineVersion;
        //当前版本号
        private String currentVersion;
    }

    @Data
    @Schema(description = "微距配置项")
    public static class DeliverInfo{
        private String deliverId;
        private String bannerId;
    }




}
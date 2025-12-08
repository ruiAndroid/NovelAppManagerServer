package com.fun.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "应用支付配置信息")
public class AppPayWithConfigDTO {
    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "普通支付配置")
    private NormalPayConfigDetail normalPay;

    @Schema(description = "订单支付配置")
    private OrderPayConfigDetail orderPay;

    @Schema(description = "续费支付配置")
    private RenewPayConfigDetail renewPay;

    @Schema(description = "抖钻支付配置")
    private DouzuanPayConfigDetail douzuanPay;

    @Schema(description = "IM支付配置")
    private ImPayConfigDetail imPay;

    @Schema(description = "微信虚拟支付配置")
    private WxVirtualPayConfigDetail wxVirtualPay;

    @Schema(description = "微信虚拟连包支付配置")
    private WxVirtualRenewPayConfigDetail wxVirtualRenewPay;
    @Data
    @Schema(description = "普通支付配置详情")
    public static class NormalPayConfigDetail {
        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "安卓支付网关")
        private Integer gatewayAndroid;

        @Schema(description = "iOS支付网关")
        private Integer gatewayIos;
    }

    @Data
    @Schema(description = "订单支付配置详情")
    public static class OrderPayConfigDetail {
        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "安卓支付网关")
        private Integer gatewayAndroid;

        @Schema(description = "iOS支付网关")
        private Integer gatewayIos;
    }

    @Data
    @Schema(description = "续费支付配置详情")
    public static class RenewPayConfigDetail {
        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "安卓支付网关")
        private Integer gatewayAndroid;

        @Schema(description = "iOS支付网关")
        private Integer gatewayIos;
    }

    @Data
    @Schema(description = "抖钻支付配置详情")
    public static class DouzuanPayConfigDetail {
        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "安卓支付网关")
        private Integer gatewayAndroid;

        @Schema(description = "iOS支付网关")
        private Integer gatewayIos;
    }


    @Data
    @Schema(description = "微信虚拟支付配置详情")
    public static class WxVirtualPayConfigDetail {
        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "安卓支付网关")
        private Integer gatewayAndroid;

        @Schema(description = "iOS支付网关")
        private Integer gatewayIos;
    }
    @Data
    @Schema(description = "微信虚拟连包支付配置详情")
    public static class WxVirtualRenewPayConfigDetail {
        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "安卓支付网关")
        private Integer gatewayAndroid;

        @Schema(description = "iOS支付网关")
        private Integer gatewayIos;
    }
    @Data
    @Schema(description = "IM支付配置详情")
    public static class ImPayConfigDetail{
        @Schema(description = "是否启用")
        private Boolean enabled;
        @Schema(description = "安卓支付网关")
        private Integer gatewayAndroid;

        @Schema(description = "iOS支付网关")
        private Integer gatewayIos;
    }
} 
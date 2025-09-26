package com.fun.novel.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@TableName("app_pay")
public class AppPay {
    
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("appid")
    @Schema(description = "小程序ID")
    @JsonProperty("appid")
    private String appid;

    @TableField("pay_type")
    @Schema(description = "支付类型：normalPay, orderPay, renewPay, douzuanPay, wxVirtualPay")
    @JsonProperty("pay_type")
    private String payType;  // normalPay, orderPay, renewPay, douzuanPay,wxVirtualPay

    @TableField("normal_pay_enabled")
    @Schema(description = "是否启用普通支付")
    @JsonProperty("normal_pay_enabled")
    private Integer normalPayEnabled;

    @TableField("normal_pay_gateway_android")
    @Schema(description = "普通支付Android网关")
    @JsonProperty("normal_pay_gateway_android")
    private Integer normalPayGatewayAndroid;

    @TableField("normal_pay_gateway_ios")
    @Schema(description = "普通支付iOS网关")
    @JsonProperty("normal_pay_gateway_ios")
    private Integer normalPayGatewayIos;

    @TableField("order_pay_enabled")
    @Schema(description = "是否启用订单支付")
    @JsonProperty("order_pay_enabled")
    private Integer orderPayEnabled;

    @TableField("order_pay_gateway_android")
    @Schema(description = "订单支付Android网关")
    @JsonProperty("order_pay_gateway_android")
    private Integer orderPayGatewayAndroid;

    @TableField("order_pay_gateway_ios")
    @Schema(description = "订单支付iOS网关")
    @JsonProperty("order_pay_gateway_ios")
    private Integer orderPayGatewayIos;

    @TableField("renew_pay_enabled")
    @Schema(description = "是否启用续费支付")
    @JsonProperty("renew_pay_enabled")
    private Integer renewPayEnabled;

    @TableField("renew_pay_gateway_android")
    @Schema(description = "续费支付Android网关")
    @JsonProperty("renew_pay_gateway_android")
    private Integer renewPayGatewayAndroid;

    @TableField("renew_pay_gateway_ios")
    @Schema(description = "续费支付iOS网关")
    @JsonProperty("renew_pay_gateway_ios")
    private Integer renewPayGatewayIos;

    @TableField("douzuan_pay_enabled")
    @Schema(description = "是否启用豆钻支付")
    @JsonProperty("douzuan_pay_enabled")
    private Integer douzuanPayEnabled;

    @TableField("douzuan_pay_gateway_android")
    @Schema(description = "豆钻支付Android网关")
    @JsonProperty("douzuan_pay_gateway_android")
    private Integer douzuanPayGatewayAndroid;

    @TableField("douzuan_pay_gateway_ios")
    @Schema(description = "豆钻支付iOS网关")
    @JsonProperty("douzuan_pay_gateway_ios")
    private Integer douzuanPayGatewayIos;

    @TableField("wx_virtual_pay_enabled")
    @Schema(description = "是否启用微信虚拟支付")
    @JsonProperty("wx_virtual_pay_enabled")
    private Integer wxVirtualPayEnabled;

    @TableField("wx_virtual_pay_gateway_android")
    @Schema(description = "微信虚拟支付Android网关")
    @JsonProperty("wx_virtual_pay_gateway_android")
    private Integer wxVirtualPayGatewayAndroid;

    @TableField("wx_virtual_pay_gateway_ios")
    @Schema(description = "微信虚拟支付iOS网关")
    @JsonProperty("wx_virtual_pay_gateway_ios")
    private Integer wxVirtualPayGatewayIos;

    @TableField("im_pay_enabled")
    @Schema(description = "是否启用IM支付")
    @JsonProperty("im_pay_enabled")
    private Integer imPayEnabled;

    @TableField("im_pay_gateway_android")
    @Schema(description = "IM支付Android网关")
    @JsonProperty("im_pay_gateway_android")
    private Integer imPayGatewayAndroid;

    @TableField("im_pay_gateway_ios")
    @Schema(description = "IM支付iOS网关")
    @JsonProperty("im_pay_gateway_ios")
    private Integer imPayGatewayIos;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    @JsonProperty("create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    @JsonProperty("update_time")
    private LocalDateTime updateTime;

    public Integer getNormalPayEnabled() {
        return normalPayEnabled == null ? 0 : normalPayEnabled;
    }

    public Integer getNormalPayGatewayAndroid() {
        return normalPayGatewayAndroid == null ? 0 : normalPayGatewayAndroid;
    }

    public Integer getNormalPayGatewayIos() {
        return normalPayGatewayIos == null ? 0 : normalPayGatewayIos;
    }

    public Integer getOrderPayEnabled() {
        return orderPayEnabled == null ? 0 : orderPayEnabled;
    }

    public Integer getOrderPayGatewayAndroid() {
        return orderPayGatewayAndroid == null ? 0 : orderPayGatewayAndroid;
    }

    public Integer getOrderPayGatewayIos() {
        return orderPayGatewayIos == null ? 0 : orderPayGatewayIos;
    }

    public Integer getRenewPayEnabled() {
        return renewPayEnabled == null ? 0 : renewPayEnabled;
    }

    public Integer getRenewPayGatewayAndroid() {
        return renewPayGatewayAndroid == null ? 0 : renewPayGatewayAndroid;
    }

    public Integer getRenewPayGatewayIos() {
        return renewPayGatewayIos == null ? 0 : renewPayGatewayIos;
    }

    public Integer getDouzuanPayEnabled() {
        return douzuanPayEnabled == null ? 0 : douzuanPayEnabled;
    }

    public Integer getDouzuanPayGatewayAndroid() {
        return douzuanPayGatewayAndroid == null ? 0 : douzuanPayGatewayAndroid;
    }

    public Integer getDouzuanPayGatewayIos() {
        return douzuanPayGatewayIos == null ? 0 : douzuanPayGatewayIos;
    }

    public Integer getWxVirtualPayEnabled() {
        return wxVirtualPayEnabled == null ? 0 : wxVirtualPayEnabled;
    }

    public Integer getWxVirtualPayGatewayAndroid() {
        return wxVirtualPayGatewayAndroid == null ? 0 : wxVirtualPayGatewayAndroid;
    }

    public Integer getWxVirtualPayGatewayIos() {
        return wxVirtualPayGatewayIos == null ? 0 : wxVirtualPayGatewayIos;
    }
} 
package com.fun.novel.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateAppPayRequest {
    
    @NotBlank(message = "appid不能为空")
    private String appId;
    
    @NotBlank(message = "支付类型不能为空")
    private String payType;
    
    @NotNull(message = "enabled不能为空")
    private Boolean enabled;
    
    @NotNull(message = "安卓支付网关不能为空")
    private Integer gatewayAndroid;
    
    @NotNull(message = "iOS支付网关不能为空")
    private Integer gatewayIos;
} 
package com.fun.novel.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@TableName("app_pay")
public class AppPay {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String appid;
    private String payType;  // normalPay, orderPay, renewPay, douzuanPay
    private Integer normalPayEnabled;
    private Integer normalPayGatewayAndroid;
    private Integer normalPayGatewayIos;
    private Integer orderPayEnabled;
    private Integer orderPayGatewayAndroid;
    private Integer orderPayGatewayIos;
    private Integer renewPayEnabled;
    private Integer renewPayGatewayAndroid;
    private Integer renewPayGatewayIos;
    private Integer douzuanPayEnabled;
    private Integer douzuanPayGatewayAndroid;
    private Integer douzuanPayGatewayIos;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 
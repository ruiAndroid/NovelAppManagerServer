package com.fun.novel.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "应用通用配置信息")
public class AppCommonConfigDTO {
    
    @NotBlank(message = "应用ID不能为空")
    @Schema(description = "应用ID")
    private String appId;
    
    @Schema(description = "联系方式")
    private String contact;
    
    @Schema(description = "抖音IM ID")
    private String douyinImId;
    
    @Schema(description = "快手Client ID")
    private String kuaishouClientId;
    
    @Schema(description = "快手Client Secret")
    private String kuaishouClientSecret;

    @Schema(description = "快手app token")
    private String kuaishouAppToken;

    @Schema(description = "微信app token")
    private String weixinAppToken;

    @Schema(description = "编译编码")
    private String buildCode;

    @Schema(description = "抖音app token")
    private String douyinAppToken;

    @Schema(description = "支付卡片样式")
    private Integer payCardStyle;


    @Schema(description = "阅读页登录类型")
    private String readerLoginType;

    @Schema(description = "我的页登录类型")
    private String mineLoginType;

    @Schema(description = "首页卡片样式")
    private Integer homeCardStyle;

    @Schema(description = "是否iaa模式")
    private Boolean iaaMode;

    @Schema(description = "iaa弹窗样式")
    private Integer iaaDialogStyle;

    @Schema(description = "是否屏蔽付费入口")
    private Boolean hidePayEntry;
}

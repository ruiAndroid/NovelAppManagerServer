package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_weiju_banner")
@Schema(description = "小说微距banner信息")
public class AppWeijuBanner {
    @TableId(type = IdType.AUTO)
    @TableField("ad_id")
    @Schema(description = "主键ID")
    private Integer adId;

    @TableField("banner_id")
    @Schema(description = "Banner ID")
    private String bannerId;

    @TableField("mat_id")
    @Schema(description = "素材ID")
    @JsonProperty("mat_id")
    private String matId;

    @TableField("material")
    @Schema(description = "素材")
    private String material;

    @TableField("time")
    @Schema(description = "时间")
    private String time;

    @TableField("ks_apple_pay")
    @Schema(description = "快手苹果支付")
    @JsonProperty("ks_apple_pay")
    private String ksApplePay;

    @TableField("channel_switch")
    @Schema(description = "渠道开关")
    @JsonProperty("channel_switch")
    private String channelSwitch;

    @TableField("reset_gray_switch")
    @Schema(description = "重置灰度开关")
    @JsonProperty("reset_gray_switch")
    private String resetGraySwitch;

    @TableField("pay_ad")
    @Schema(description = "支付广告")
    @JsonProperty("pay_ad")
    private String payAd;

    @TableField("renew_pay")
    @Schema(description = "续费支付")
    @JsonProperty("renew_pay")
    private String renewPay;

    @TableField("bd_apple_pay")
    @Schema(description = "百度苹果支付")
    @JsonProperty("bd_apple_pay")
    private String bdApplePay;

    @TableField("tt_iOS_pay")
    @Schema(description = "头条iOS支付")
    @JsonProperty("tt_iOS_pay")
    private String ttIosPay;

    @TableField("wx_iOS_pay")
    @Schema(description = "微信iOS支付")
    @JsonProperty("wx_iOS_pay")
    private String wxIosPay;

    @TableField("pay_method")
    @Schema(description = "支付方式")
    @JsonProperty("pay_method")
    private String payMethod;

    @TableField("tt_apple_pay")
    @Schema(description = "头条苹果支付")
    @JsonProperty("tt_apple_pay")
    private String ttApplePay;

    @TableField("is_im")
    @Schema(description = "是否IM")
    @JsonProperty("is_im")
    private String isIm;

    @TableField("homepage_pop_01")
    @Schema(description = "首页弹窗01")
    @JsonProperty("homepage_pop_01")
    private String homepagePop01;

    @TableField("homepage_banner_01")
    @Schema(description = "首页Banner01")
    @JsonProperty("homepage_banner_01")
    private String homepageBanner01;

    @TableField("homepage_banner_02")
    @JsonProperty("homepage_banner_02")
    private String homepageBanner02;

    @TableField("homepage_banner_03")
    @JsonProperty("homepage_banner_03")
    private String homepageBanner03;

    @TableField("homepage_banner_04")
    @JsonProperty("homepage_banner_04")
    private String homepageBanner04;

    @TableField("homepage_banner_05")
    @JsonProperty("homepage_banner_05")
    private String homepageBanner05;

    @TableField("homepage_banner_06")
    @JsonProperty("homepage_banner_06")
    private String homepageBanner06;

    @TableField("homepage_banner_07")
    @JsonProperty("homepage_banner_07")
    private String homepageBanner07;

    @TableField("homepage_banner_08")
    @JsonProperty("homepage_banner_08")
    private String homepageBanner08;

    @TableField("detailPage_banner_01")
    @Schema(description = "详情页Banner01")
    @JsonProperty("detailPage_banner_01")
    private String detailPageBanner01;

    @TableField("detailPage_banner_02")
    @JsonProperty("detailPage_banner_02")
    private String detailPageBanner02;

    @TableField("mediapage_video_banner")
    @Schema(description = "媒体页视频Banner")
    @JsonProperty("mediapage_video_banner")
    private String mediapageVideoBanner;

    @TableField("mediapage_banner_01")
    @JsonProperty("mediapage_banner_01")
    private String mediapageBanner01;

    @TableField("mediapage_banner_02")
    @JsonProperty("mediapage_banner_02")
    private String mediapageBanner02;

    @TableField("mediapage_banner_03")
    @JsonProperty("mediapage_banner_03")
    private String mediapageBanner03;

    @TableField("mediapage_banner_04")
    @JsonProperty("mediapage_banner_04")
    private String mediapageBanner04;

    @TableField("mediapage_banner_05")
    @JsonProperty("mediapage_banner_05")
    private String mediapageBanner05;

    @TableField("mediapage_banner_06")
    @JsonProperty("mediapage_banner_06")
    private String mediapageBanner06;

    @TableField("mediapage_banner_07")
    @JsonProperty("mediapage_banner_07")
    private String mediapageBanner07;

    @TableField("mediapage_banner_08")
    @JsonProperty("mediapage_banner_08")
    private String mediapageBanner08;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    @JsonProperty("create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    @JsonProperty("update_time")
    private LocalDateTime updateTime;

    public String getBannerId() {
        return bannerId == null ? "" : bannerId;
    }

    public String getMatId() {
        return matId == null ? "" : matId;
    }

    public String getMaterial() {
        return material == null ? "" : material;
    }

    public String getTime() {
        return time == null ? "" : time;
    }

    public String getKsApplePay() {
        return ksApplePay == null ? "" : ksApplePay;
    }

    public String getChannelSwitch() {
        return channelSwitch == null ? "" : channelSwitch;
    }

    public String getResetGraySwitch() {
        return resetGraySwitch == null ? "" : resetGraySwitch;
    }

    public String getPayAd() {
        return payAd == null ? "" : payAd;
    }

    public String getRenewPay() {
        return renewPay == null ? "" : renewPay;
    }

    public String getBdApplePay() {
        return bdApplePay == null ? "" : bdApplePay;
    }

    public String getTtIosPay() {
        return ttIosPay == null ? "" : ttIosPay;
    }

    public String getWxIosPay() {
        return wxIosPay == null ? "" : wxIosPay;
    }

    public String getPayMethod() {
        return payMethod == null ? "" : payMethod;
    }

    public String getTtApplePay() {
        return ttApplePay == null ? "" : ttApplePay;
    }

    public String getIsIm() {
        return isIm == null ? "" : isIm;
    }

    public String getHomepagePop01() {
        return homepagePop01 == null ? "" : homepagePop01;
    }

    public String getHomepageBanner01() {
        return homepageBanner01 == null ? "" : homepageBanner01;
    }

    public String getHomepageBanner02() {
        return homepageBanner02 == null ? "" : homepageBanner02;
    }

    public String getHomepageBanner03() {
        return homepageBanner03 == null ? "" : homepageBanner03;
    }

    public String getHomepageBanner04() {
        return homepageBanner04 == null ? "" : homepageBanner04;
    }

    public String getHomepageBanner05() {
        return homepageBanner05 == null ? "" : homepageBanner05;
    }

    public String getHomepageBanner06() {
        return homepageBanner06 == null ? "" : homepageBanner06;
    }

    public String getHomepageBanner07() {
        return homepageBanner07 == null ? "" : homepageBanner07;
    }

    public String getHomepageBanner08() {
        return homepageBanner08 == null ? "" : homepageBanner08;
    }

    public String getDetailPageBanner01() {
        return detailPageBanner01 == null ? "" : detailPageBanner01;
    }

    public String getDetailPageBanner02() {
        return detailPageBanner02 == null ? "" : detailPageBanner02;
    }

    public String getMediapageVideoBanner() {
        return mediapageVideoBanner == null ? "" : mediapageVideoBanner;
    }

    public String getMediapageBanner01() {
        return mediapageBanner01 == null ? "" : mediapageBanner01;
    }

    public String getMediapageBanner02() {
        return mediapageBanner02 == null ? "" : mediapageBanner02;
    }

    public String getMediapageBanner03() {
        return mediapageBanner03 == null ? "" : mediapageBanner03;
    }

    public String getMediapageBanner04() {
        return mediapageBanner04 == null ? "" : mediapageBanner04;
    }

    public String getMediapageBanner05() {
        return mediapageBanner05 == null ? "" : mediapageBanner05;
    }

    public String getMediapageBanner06() {
        return mediapageBanner06 == null ? "" : mediapageBanner06;
    }

    public String getMediapageBanner07() {
        return mediapageBanner07 == null ? "" : mediapageBanner07;
    }

    public String getMediapageBanner08() {
        return mediapageBanner08 == null ? "" : mediapageBanner08;
    }
} 
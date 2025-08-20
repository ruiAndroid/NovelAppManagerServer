package com.fun.novel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@TableName("app_weiju_deliver")
@Schema(description = "小说微距deliver信息")
public class AppWeijuDeliver {
    @TableId(value = "ad_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer adId;

    @NotBlank(message = "deliverId不能为空")
    @TableField("deliver_id")
    @Schema(description = "deliverID", required = true)
    private String deliverId;

    @TableField("coin_si")
    @Schema(description = "金币数")
    @JsonProperty("coin_si")
    private String coinSi;

    @TableField("package_si")
    @Schema(description = "包名")
    @JsonProperty("package_si")
    private String packageSi;

    @TableField("freeX_perUnlock")
    @Schema(description = "免费解锁次数")
    @JsonProperty("freeX_perUnlock")
    private String freeXPerUnlock;

    @TableField("material")
    @Schema(description = "素材")
    private String material;

    @TableField("ok")
    @Schema(description = "是否有效")
    private String ok;

    @TableField("priority_payment")
    @Schema(description = "优先支付")
    @JsonProperty("priority_payment")
    private String priorityPayment;

    @TableField("x_unlock")
    @Schema(description = "解锁X")
    @JsonProperty("x_unlock")
    private String xUnlock;

    @TableField("parm2")
    @Schema(description = "参数2")
    private String parm2;

    @TableField("free_step_1")
    @Schema(description = "免费步骤1")
    @JsonProperty("free_step_1")
    private String freeStep1;

    @TableField("free_step_2")
    @Schema(description = "免费步骤2")
    @JsonProperty("free_step_2")
    private String freeStep2;

    @TableField("free_step_3")
    @Schema(description = "免费步骤3")
    @JsonProperty("free_step_3")
    private String freeStep3;

    @TableField("free_step_4")
    @Schema(description = "免费步骤4")
    @JsonProperty("free_step_4")
    private String freeStep4;

    @TableField("vip_step_1")
    @Schema(description = "VIP步骤1")
    @JsonProperty("vip_step_1")
    private String vipStep1;

    @TableField("vip_step_2")
    @Schema(description = "VIP步骤2")
    @JsonProperty("vip_step_2")
    private String vipStep2;

    @TableField("vip_step_3")
    @Schema(description = "VIP步骤3")
    @JsonProperty("vip_step_3")
    private String vipStep3;

    @TableField("vip_step_4")
    @Schema(description = "VIP步骤4")
    @JsonProperty("vip_step_4")
    private String vipStep4;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    @JsonProperty("create_time")
    @JsonIgnore
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    @JsonProperty("update_time")
    @JsonIgnore
    private LocalDateTime updateTime;

    public String getDeliverId() {
        return deliverId == null ? "" : deliverId;
    }

    public String getCoinSi() {
        return coinSi == null ? "" : coinSi;
    }

    public String getPackageSi() {
        return packageSi == null ? "" : packageSi;
    }

    public String getFreeXPerUnlock() {
        return freeXPerUnlock == null ? "" : freeXPerUnlock;
    }

    public String getMaterial() {
        return material == null ? "" : material;
    }

    public String getOk() {
        return ok == null ? "" : ok;
    }

    public String getPriorityPayment() {
        return priorityPayment == null ? "" : priorityPayment;
    }

    public String getXUnlock() {
        return xUnlock == null ? "" : xUnlock;
    }

    public String getParm2() {
        return parm2 == null ? "" : parm2;
    }

    public String getFreeStep1() {
        return freeStep1 == null ? "" : freeStep1;
    }

    public String getFreeStep2() {
        return freeStep2 == null ? "" : freeStep2;
    }

    public String getFreeStep3() {
        return freeStep3 == null ? "" : freeStep3;
    }

    public String getFreeStep4() {
        return freeStep4 == null ? "" : freeStep4;
    }

    public String getVipStep1() {
        return vipStep1 == null ? "" : vipStep1;
    }

    public String getVipStep2() {
        return vipStep2 == null ? "" : vipStep2;
    }

    public String getVipStep3() {
        return vipStep3 == null ? "" : vipStep3;
    }

    public String getVipStep4() {
        return vipStep4 == null ? "" : vipStep4;
    }
} 
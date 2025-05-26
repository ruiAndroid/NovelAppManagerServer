package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.AppPay;
import com.fun.novel.mapper.AppPayMapper;
import com.fun.novel.service.AppPayService;
import com.fun.novel.dto.CreateAppPayRequest;
import com.fun.novel.dto.UpdateAppPayRequest;
import com.fun.novel.dto.AppPayWithConfigDTO;
import com.fun.novel.dto.AppPayWithConfigDTO.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AppPayServiceImpl extends ServiceImpl<AppPayMapper, AppPay> implements AppPayService {

    private static final List<String> VALID_PAY_TYPES = Arrays.asList(
            "normalPay", "orderPay", "renewPay", "douzuanPay", "wxVirtualPay");

    @Override
    public AppPayWithConfigDTO createAppPay(CreateAppPayRequest request) {
        // 验证支付类型是否合法
        if (!VALID_PAY_TYPES.contains(request.getPayType())) {
            throw new IllegalArgumentException("不支持的支付类型");
        }

        // 检查是否已存在相同appid和payType的配置
        QueryWrapper<AppPay> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("appid", request.getAppId())
                .eq("pay_type", request.getPayType());
        if (getOne(queryWrapper) != null) {
            throw new IllegalArgumentException("该小程序已存在相同类型的支付配置");
        }

        // 验证网关参数
        if (request.getGatewayAndroid() < 0 || request.getGatewayIos() < 0) {
            throw new IllegalArgumentException("支付网关参数不能为负数");
        }

        AppPay appPay = new AppPay();
        appPay.setAppid(request.getAppId());
        appPay.setPayType(request.getPayType());
        appPay.setCreateTime(LocalDateTime.now());
        appPay.setUpdateTime(LocalDateTime.now());

        // 将Boolean转换为Integer
        Integer enabledValue = request.getEnabled() ? 1 : 0;

        // 根据支付类型设置对应的字段
        switch (request.getPayType()) {
            case "normalPay":
                appPay.setNormalPayEnabled(enabledValue);
                appPay.setNormalPayGatewayAndroid(request.getGatewayAndroid());
                appPay.setNormalPayGatewayIos(request.getGatewayIos());
                break;
            case "orderPay":
                appPay.setOrderPayEnabled(enabledValue);
                appPay.setOrderPayGatewayAndroid(request.getGatewayAndroid());
                appPay.setOrderPayGatewayIos(request.getGatewayIos());
                break;
            case "renewPay":
                appPay.setRenewPayEnabled(enabledValue);
                appPay.setRenewPayGatewayAndroid(request.getGatewayAndroid());
                appPay.setRenewPayGatewayIos(request.getGatewayIos());
                break;
            case "douzuanPay":
                appPay.setDouzuanPayEnabled(enabledValue);
                appPay.setDouzuanPayGatewayAndroid(request.getGatewayAndroid());
                appPay.setDouzuanPayGatewayIos(request.getGatewayIos());
                break;
            case "wxVirtualPay":
                appPay.setWxVirtualPayEnabled(enabledValue);
                appPay.setWxVirtualPayGatewayAndroid(request.getGatewayAndroid());
                appPay.setWxVirtualPayGatewayIos(request.getGatewayIos());
                break;
        }

        save(appPay);

        // 返回完整的配置信息
        return getAppPayByAppId(request.getAppId());
    }

    @Override
    public AppPayWithConfigDTO getAppPayByAppId(String appid) {
        QueryWrapper<AppPay> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("appid", appid);
        List<AppPay> appPays = list(queryWrapper);

        if (appPays.isEmpty()) {
            return null;
        }

        AppPayWithConfigDTO result = new AppPayWithConfigDTO();
        result.setAppId(appid);

        for (AppPay appPay : appPays) {
            switch (appPay.getPayType()) {
                case "normalPay":
                    NormalPayConfigDetail normalConfig = new NormalPayConfigDetail();
                    normalConfig.setEnabled(appPay.getNormalPayEnabled() == 1);
                    normalConfig.setGatewayAndroid(appPay.getNormalPayGatewayAndroid());
                    normalConfig.setGatewayIos(appPay.getNormalPayGatewayIos());
                    result.setNormalPay(normalConfig);
                    break;
                case "orderPay":
                    OrderPayConfigDetail orderConfig = new OrderPayConfigDetail();
                    orderConfig.setEnabled(appPay.getOrderPayEnabled() == 1);
                    orderConfig.setGatewayAndroid(appPay.getOrderPayGatewayAndroid());
                    orderConfig.setGatewayIos(appPay.getOrderPayGatewayIos());
                    result.setOrderPay(orderConfig);
                    break;
                case "renewPay":
                    RenewPayConfigDetail renewConfig = new RenewPayConfigDetail();
                    renewConfig.setEnabled(appPay.getRenewPayEnabled() == 1);
                    renewConfig.setGatewayAndroid(appPay.getRenewPayGatewayAndroid());
                    renewConfig.setGatewayIos(appPay.getRenewPayGatewayIos());
                    result.setRenewPay(renewConfig);
                    break;
                case "douzuanPay":
                    DouzuanPayConfigDetail douzuanConfig = new DouzuanPayConfigDetail();
                    douzuanConfig.setEnabled(appPay.getDouzuanPayEnabled() == 1);
                    douzuanConfig.setGatewayAndroid(appPay.getDouzuanPayGatewayAndroid());
                    douzuanConfig.setGatewayIos(appPay.getDouzuanPayGatewayIos());
                    result.setDouzuanPay(douzuanConfig);
                    break;
                case "wxVirtualPay":
                    WxVirtualPayConfigDetail wxVirtualPayConfig = new WxVirtualPayConfigDetail();
                    wxVirtualPayConfig.setEnabled(appPay.getWxVirtualPayEnabled() == 1);
                    wxVirtualPayConfig.setGatewayAndroid(appPay.getWxVirtualPayGatewayAndroid());
                    wxVirtualPayConfig.setGatewayIos(appPay.getWxVirtualPayGatewayIos());
                    result.setWxVirtualPay(wxVirtualPayConfig);
            }
        }

        return result;
    }

    @Override
    public AppPayWithConfigDTO updateAppPay(UpdateAppPayRequest request) {
        // 验证支付类型是否合法
        if (!VALID_PAY_TYPES.contains(request.getPayType())) {
            throw new IllegalArgumentException("不支持的支付类型");
        }

        // 验证网关参数
        if (request.getGatewayAndroid() < 0 || request.getGatewayIos() < 0) {
            throw new IllegalArgumentException("支付网关参数不能为负数");
        }

        // 查找现有配置
        AppPay existingAppPay = getOne(new QueryWrapper<AppPay>()
                .eq("appid", request.getAppId())
                .eq("pay_type", request.getPayType()));

        if (existingAppPay == null) {
            throw new IllegalArgumentException("该小程序支付配置不存在");
        }

        // 将Boolean转换为Integer
        Integer enabledValue = request.getEnabled() ? 1 : 0;

        // 根据支付类型更新对应的字段
        switch (request.getPayType()) {
            case "normalPay":
                existingAppPay.setNormalPayEnabled(enabledValue);
                existingAppPay.setNormalPayGatewayAndroid(request.getGatewayAndroid());
                existingAppPay.setNormalPayGatewayIos(request.getGatewayIos());
                break;
            case "orderPay":
                existingAppPay.setOrderPayEnabled(enabledValue);
                existingAppPay.setOrderPayGatewayAndroid(request.getGatewayAndroid());
                existingAppPay.setOrderPayGatewayIos(request.getGatewayIos());
                break;
            case "renewPay":
                existingAppPay.setRenewPayEnabled(enabledValue);
                existingAppPay.setRenewPayGatewayAndroid(request.getGatewayAndroid());
                existingAppPay.setRenewPayGatewayIos(request.getGatewayIos());
                break;
            case "douzuanPay":
                existingAppPay.setDouzuanPayEnabled(enabledValue);
                existingAppPay.setDouzuanPayGatewayAndroid(request.getGatewayAndroid());
                existingAppPay.setDouzuanPayGatewayIos(request.getGatewayIos());
            case "wxVirtualPay":
                existingAppPay.setWxVirtualPayEnabled(enabledValue);
                existingAppPay.setWxVirtualPayGatewayAndroid(request.getGatewayAndroid());
                existingAppPay.setWxVirtualPayGatewayIos(request.getGatewayIos());
                break;
        }

        existingAppPay.setUpdateTime(LocalDateTime.now());
        updateById(existingAppPay);

        // 返回完整的配置信息
        return getAppPayByAppId(request.getAppId());
    }

    @Override
    public boolean deleteAppPayByAppIdAndType(String appId, String payType) {
        // 验证支付类型是否合法
        if (!VALID_PAY_TYPES.contains(payType)) {
            throw new IllegalArgumentException("不支持的支付类型");
        }

        QueryWrapper<AppPay> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("appid", appId).eq("pay_type", payType);

        return remove(queryWrapper);
    }

    @Override
    public boolean deleteAppPayByAppId(String appId) {
        LambdaQueryWrapper<AppPay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppPay::getAppid, appId);
        return remove(wrapper);
    }
}
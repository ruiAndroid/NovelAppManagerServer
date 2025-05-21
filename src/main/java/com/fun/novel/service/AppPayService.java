package com.fun.novel.service;

import com.fun.novel.entity.AppPay;
import com.fun.novel.dto.CreateAppPayRequest;
import com.fun.novel.dto.UpdateAppPayRequest;
import com.fun.novel.dto.AppPayWithConfigDTO;

public interface AppPayService {
    
    /**
     * Create a new app pay configuration
     */
    AppPayWithConfigDTO createAppPay(CreateAppPayRequest request);
    
    /**
     * Get app pay configuration by appid
     */
    AppPayWithConfigDTO getAppPayByAppId(String appid);
    
    /**
     * Update app pay configuration
     */
    AppPayWithConfigDTO updateAppPay(UpdateAppPayRequest request);
    
    /**
     * Delete app pay configuration by appId and payType
     */
    boolean deleteAppPayByAppIdAndType(String appId, String payType);
    
    /**
     * 删除应用的所有支付配置
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteAppPayByAppId(String appId);
} 
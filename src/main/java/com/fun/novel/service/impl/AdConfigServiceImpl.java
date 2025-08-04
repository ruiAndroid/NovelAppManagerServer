package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.entity.AdConfig;
import com.fun.novel.entity.AppAd;
import com.fun.novel.mapper.AdConfigMapper;
import com.fun.novel.mapper.AppAdMapper;
import com.fun.novel.service.AdConfigService;
import com.fun.novel.dto.UpdateAdConfigRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdConfigServiceImpl implements AdConfigService {

    @Autowired
    private AdConfigMapper adConfigMapper;

    @Autowired
    private AppAdMapper appAdMapper;

    // 定义支持的广告类型集合
    private static final Set<String> SUPPORTED_AD_TYPES = new HashSet<>();

    static {
        SUPPORTED_AD_TYPES.add("reward");
        SUPPORTED_AD_TYPES.add("interstitial");
        SUPPORTED_AD_TYPES.add("banner");
        SUPPORTED_AD_TYPES.add("feed");
    }
    @Override
    @Transactional
    public AdConfig addAdConfig(AdConfig adConfig) {

        // 检查广告类型是否支持
        if (adConfig.getAdType() == null || !SUPPORTED_AD_TYPES.contains(adConfig.getAdType().toLowerCase())) {
            throw new IllegalArgumentException("不支持的广告类型");
        }

        // 检查是否已经存在相同app_ad_id和ad_type的记录
        LambdaQueryWrapper<AdConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AdConfig::getAppAdId, adConfig.getAppAdId())
                   .eq(AdConfig::getAdType, adConfig.getAdType());
        
        AdConfig existingConfig = adConfigMapper.selectOne(queryWrapper);
        if (existingConfig != null) {
            throw new IllegalArgumentException("该应用下已存在相同类型的广告配置");
        }

        // 对reward类型进行特殊验证
        if ("reward".equalsIgnoreCase(adConfig.getAdType())) {
            // 验证rewardAdId
            if (adConfig.getRewardAdId() == null || adConfig.getRewardAdId().trim().isEmpty()) {
                throw new IllegalArgumentException("reward类型的广告ID不能为空");
            }

            // 验证rewardCount
            if (adConfig.getRewardCount() == null) {
                throw new IllegalArgumentException("reward类型的奖励次数不能为空");
            }
            if (adConfig.getRewardCount() < 0) {
                throw new IllegalArgumentException("reward类型的奖励次数不能为负数");
            }

            // 验证isRewardAdEnabled
            if (adConfig.getIsRewardAdEnabled() == null) {
                throw new IllegalArgumentException("reward类型的启用状态不能为空");
            }
        }


        // 对interstitial类型进行特殊验证
        if ("interstitial".equalsIgnoreCase(adConfig.getAdType())) {
            // 验证interstitialAdId
            if (adConfig.getInterstitialAdId() == null || adConfig.getInterstitialAdId().trim().isEmpty()) {
                throw new IllegalArgumentException("interstitial类型的广告ID不能为空");
            }

            // 验证interstitialCount
            if (adConfig.getInterstitialCount() == null) {
                throw new IllegalArgumentException("interstitial类型的奖励次数不能为空");
            }
            if (adConfig.getInterstitialCount() < 0) {
                throw new IllegalArgumentException("interstitial类型的奖励次数不能为负数");
            }

            // 验证isInterstitialAdEnabled
            if (adConfig.getIsInterstitialAdEnabled() == null) {
                throw new IllegalArgumentException("interstitial类型的启用状态不能为空");
            }
        }

        //对banner类型进行特殊验证
        if ("banner".equalsIgnoreCase(adConfig.getAdType())) {
            if (adConfig.getBannerAdId() == null || adConfig.getBannerAdId().trim().isEmpty()) {
                throw new IllegalArgumentException("banner类型的广告ID不能为空");
            }

            if (adConfig.getIsBannerAdEnabled() == null) {
                throw new IllegalArgumentException("banner类型的启用状态不能为空");
            }
        }

        //对feed类型进行特殊验证
        if ("feed".equalsIgnoreCase(adConfig.getAdType())) {
            if (adConfig.getFeedAdId() == null || adConfig.getFeedAdId().trim().isEmpty()) {
                throw new IllegalArgumentException("feed类型的广告ID不能为空");
            }
            if (adConfig.getIsFeedAdEnabled() == null) {
                throw new IllegalArgumentException("feed类型的启用状态不能为空");
            }
        }
        adConfigMapper.insert(adConfig);
        return adConfig;
    }

    @Override
    @Transactional
    public AdConfig updateAdConfig(UpdateAdConfigRequest request) {

        // 查询对应的广告配置
        LambdaQueryWrapper<AdConfig> configQueryWrapper = new LambdaQueryWrapper<>();
        configQueryWrapper.eq(AdConfig::getAppAdId, request.getAppAdId())
                         .eq(AdConfig::getAdType, request.getAdType());
        AdConfig adConfig = adConfigMapper.selectOne(configQueryWrapper);

        if (adConfig == null) {
            throw new IllegalArgumentException("未找到对应的广告配置记录");
        }

        // 对reward类型进行特殊验证
        if ("reward".equalsIgnoreCase(request.getAdType())) {
            // 验证rewardAdId
            if (request.getRewardAdId() == null || request.getRewardAdId().trim().isEmpty()) {
                throw new IllegalArgumentException("reward类型的广告ID不能为空");
            }

            // 验证rewardCount
            if (request.getRewardCount() == null) {
                throw new IllegalArgumentException("reward类型的奖励次数不能为空");
            }
            if (request.getRewardCount() < 0) {
                throw new IllegalArgumentException("reward类型的奖励次数不能为负数");
            }

            // 验证isRewardAdEnabled
            if (request.getIsRewardAdEnabled() == null) {
                throw new IllegalArgumentException("reward类型的启用状态不能为空");
            }

            // 更新配置
            adConfig.setRewardAdId(request.getRewardAdId());
            adConfig.setRewardCount(request.getRewardCount());
            adConfig.setIsRewardAdEnabled(request.getIsRewardAdEnabled());
        }

        // 对interstitial类型进行特殊验证
        if ("interstitial".equalsIgnoreCase(adConfig.getAdType())) {
            // 验证interstitialAdId
            if (adConfig.getInterstitialAdId() == null || adConfig.getInterstitialAdId().trim().isEmpty()) {
                throw new IllegalArgumentException("interstitial类型的广告ID不能为空");
            }

            // 验证interstitialCount
            if (adConfig.getInterstitialCount() == null) {
                throw new IllegalArgumentException("interstitial类型的奖励次数不能为空");
            }
            if (adConfig.getInterstitialCount() < 0) {
                throw new IllegalArgumentException("interstitial类型的奖励次数不能为负数");
            }

            // 验证isInterstitialAdEnabled
            if (adConfig.getIsInterstitialAdEnabled() == null) {
                throw new IllegalArgumentException("interstitial类型的启用状态不能为空");
            }

            // 更新配置
            adConfig.setInterstitialAdId(request.getInterstitialAdId());
            adConfig.setInterstitialCount(request.getInterstitialCount());
            adConfig.setIsInterstitialAdEnabled(request.getIsInterstitialAdEnabled());
        }

        // 对banner类型进行特殊验证
        if ("banner".equalsIgnoreCase(adConfig.getAdType())) {
            // 验证bannerAdId
            if (adConfig.getBannerAdId() == null || adConfig.getBannerAdId().trim().isEmpty()) {
                throw new IllegalArgumentException("banner类型的广告ID不能为空");
            }


            // 验证isBannerAdEnabled
            if (adConfig.getIsBannerAdEnabled() == null) {
                throw new IllegalArgumentException("banner类型的启用状态不能为空");
            }

            // 更新配置
            adConfig.setBannerAdId(request.getBannerAdId());
            adConfig.setIsBannerAdEnabled(request.getIsBannerAdEnabled());
        }

        //对feed类型进行特殊验证

        if ("feed".equalsIgnoreCase(adConfig.getAdType())) {
            // 验证feedAdId
            if (adConfig.getFeedAdId() == null || adConfig.getFeedAdId().trim().isEmpty()) {
                throw new IllegalArgumentException("feed类型的广告ID不能为空");
            }

            // 验证isFeedAdEnabled
            if (adConfig.getIsFeedAdEnabled() == null) {
                throw new IllegalArgumentException("feed类型的启用状态不能为空");
            }

            // 更新配置
            adConfig.setFeedAdId(request.getFeedAdId());
            adConfig.setIsFeedAdEnabled(request.getIsFeedAdEnabled());
        }

        adConfigMapper.updateById(adConfig);
        return adConfig;
    }

    @Override
    @Transactional
    public boolean deleteAdConfigByAppAdIdAndType(Integer appAdId, String adType) {
        // 直接删除对应的广告配置
        LambdaQueryWrapper<AdConfig> configQueryWrapper = new LambdaQueryWrapper<>();
        configQueryWrapper.eq(AdConfig::getAppAdId, appAdId)
                         .eq(AdConfig::getAdType, adType);
        
        int rows = adConfigMapper.delete(configQueryWrapper);
        return rows > 0;
    }

} 
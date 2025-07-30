package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.entity.AppAd;
import com.fun.novel.entity.AdConfig;
import com.fun.novel.mapper.AppAdMapper;
import com.fun.novel.mapper.AdConfigMapper;
import com.fun.novel.service.AppAdService;
import com.fun.novel.dto.AppAdWithConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppAdServiceImpl implements AppAdService {

    @Autowired
    private AppAdMapper appAdMapper;

    @Autowired
    private AdConfigMapper adConfigMapper;

    @Override
    @Transactional
    public AppAd addAppAd(AppAd appAd) {
        // 检查是否已经存在相同的 appid
        LambdaQueryWrapper<AppAd> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppAd::getAppid, appAd.getAppid());
        AppAd existingAppAd = appAdMapper.selectOne(queryWrapper);
        
        if (existingAppAd != null) {
            throw new IllegalArgumentException("该 appid 已经存在对应的 AppAd 记录");
        }
        
        appAdMapper.insert(appAd);
        return appAd;
    }

    @Override
    @Transactional
    public AppAd updateAppAd(AppAd appAd) {
        appAdMapper.updateById(appAd);
        return appAd;
    }

    @Override
    @Transactional
    public boolean deleteAppAdByAppId(String appId) {
        // 查询要删除的 AppAd
        LambdaQueryWrapper<AppAd> appAdQueryWrapper = new LambdaQueryWrapper<>();
        appAdQueryWrapper.eq(AppAd::getAppid, appId);
        AppAd appAd = appAdMapper.selectOne(appAdQueryWrapper);

        if (appAd == null) {
            return false; // 未找到对应的 AppAd 记录
        }

        // 删除关联的 AdConfig 记录
        LambdaQueryWrapper<AdConfig> adConfigQueryWrapper = new LambdaQueryWrapper<>();
        adConfigQueryWrapper.eq(AdConfig::getAppAdId, appAd.getId());
        adConfigMapper.delete(adConfigQueryWrapper);

        // 删除 AppAd 记录
        int rows = appAdMapper.delete(appAdQueryWrapper);
        return rows > 0;
    }

    @Override
    public AppAdWithConfigDTO getAppAdByAppId(String appId) {
        LambdaQueryWrapper<AppAd> appAdQueryWrapper = new LambdaQueryWrapper<>();
        appAdQueryWrapper.eq(AppAd::getAppid, appId);
        AppAd appAd = appAdMapper.selectOne(appAdQueryWrapper);

        if (appAd == null) {
            return null; // 未找到对应的 AppAd 记录
        }

        // 查询关联的 AdConfig 记录
        LambdaQueryWrapper<AdConfig> adConfigQueryWrapper = new LambdaQueryWrapper<>();
        adConfigQueryWrapper.eq(AdConfig::getAppAdId, appAd.getId());
        List<AdConfig> adConfigs = adConfigMapper.selectList(adConfigQueryWrapper);

        // 构建返回对象
        AppAdWithConfigDTO dto = new AppAdWithConfigDTO();
        dto.setId(appAd.getId());
        dto.setAppId(appAd.getAppid());

        for (AdConfig config : adConfigs) {


            switch (config.getAdType()) {
                case "reward":
                    AppAdWithConfigDTO.RewardAdConfigDetail rewardAdConfigDetail = new AppAdWithConfigDTO.RewardAdConfigDetail();
                    rewardAdConfigDetail.setRewardAdId(config.getRewardAdId());
                    rewardAdConfigDetail.setRewardCount(config.getRewardCount());
                    rewardAdConfigDetail.setIsRewardAdEnabled(config.getIsRewardAdEnabled());
                    dto.setReward(rewardAdConfigDetail);
                    break;
                case "interstitial":
                    AppAdWithConfigDTO.InterstitialAdConfigDetail interstitialAdConfigDetail = new AppAdWithConfigDTO.InterstitialAdConfigDetail();
                    interstitialAdConfigDetail.setInterstitialAdId(config.getInterstitialAdId());
                    interstitialAdConfigDetail.setInterstitialCount(config.getInterstitialCount());
                    interstitialAdConfigDetail.setIsInterstitialAdEnabled(config.getIsInterstitialAdEnabled());
                    dto.setInterstitial(interstitialAdConfigDetail);
                    break;
                case "banner":
                    AppAdWithConfigDTO.BannerAdConfigDetail bannerAdConfigDetail = new AppAdWithConfigDTO.BannerAdConfigDetail();
                    bannerAdConfigDetail.setBannerAdId(config.getBannerAdId());
                    bannerAdConfigDetail.setIsBannerAdEnabled(config.getIsBannerAdEnabled());
                    dto.setBanner(bannerAdConfigDetail);
                    break;
            }
        }

        return dto;
    }

    @Override
    public List<AppAd> getAllAppAds() {
        return appAdMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public String getAppIdByAppAdId(Integer appAdId) {
        if (appAdId == null) return null;
        AppAd appAd = appAdMapper.selectById(appAdId);
        return appAd != null ? appAd.getAppid() : null;
    }
} 
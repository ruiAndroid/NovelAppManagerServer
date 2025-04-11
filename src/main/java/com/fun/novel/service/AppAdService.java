package com.fun.novel.service;

import com.fun.novel.dto.AppAdWithConfigDTO;
import com.fun.novel.entity.AppAd;
import java.util.List;

public interface AppAdService {
    AppAd addAppAd(AppAd appAd);
    AppAd updateAppAd(AppAd appAd);
    boolean deleteAppAdByAppId(String appId);
    AppAdWithConfigDTO getAppAdByAppId(String appId);
    List<AppAd> getAllAppAds();
} 
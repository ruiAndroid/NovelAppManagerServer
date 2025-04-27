package com.fun.novel.service;

import com.fun.novel.entity.AppWeijuBanner;
import java.util.List;

public interface AppWeijuBannerService {
    AppWeijuBanner addBanner(AppWeijuBanner banner);
    List<AppWeijuBanner> getBannerList();
    AppWeijuBanner updateBanner(AppWeijuBanner banner);
    AppWeijuBanner getBannerByBannerId(String bannerId);
    boolean deleteBannerByBannerId(String bannerId);
} 
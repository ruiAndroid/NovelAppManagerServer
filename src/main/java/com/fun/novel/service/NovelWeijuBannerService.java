package com.fun.novel.service;

import com.fun.novel.entity.NovelWeijuBanner;
import java.util.List;

public interface NovelWeijuBannerService {
    NovelWeijuBanner addBanner(NovelWeijuBanner banner);
    List<NovelWeijuBanner> getBannerList();
    NovelWeijuBanner updateBanner(NovelWeijuBanner banner);
    NovelWeijuBanner getBannerByBannerId(String bannerId);
    boolean deleteBannerByBannerId(String bannerId);
} 
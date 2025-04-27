package com.fun.novel.dto;

import com.fun.novel.entity.AppWeijuBanner;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BannerAppResponse {
    private String ap;
    private List<AppWeijuBanner> ad_list = new ArrayList<>();

    public static BannerAppResponse fromBanner(AppWeijuBanner banner) {
        BannerAppResponse response = new BannerAppResponse();
        response.setAp(banner.getBannerId());
        response.getAd_list().add(banner);
        return response;
    }
} 
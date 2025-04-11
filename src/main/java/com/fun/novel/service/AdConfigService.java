package com.fun.novel.service;

import com.fun.novel.entity.AdConfig;
import com.fun.novel.dto.UpdateAdConfigRequest;
import java.util.List;

public interface AdConfigService {
    AdConfig addAdConfig(AdConfig adConfig);
    AdConfig updateAdConfig(UpdateAdConfigRequest request);
    boolean deleteAdConfigByAppAdIdAndType(Integer appAdId, String adType);
}
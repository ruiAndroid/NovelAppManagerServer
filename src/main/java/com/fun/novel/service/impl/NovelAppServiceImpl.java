package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.mapper.NovelAppMapper;
import com.fun.novel.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelAppServiceImpl extends ServiceImpl<NovelAppMapper, NovelApp> implements NovelAppService {

    private final AppAdService appAdService;
    private final AppPayService appPayService;
    private final AppWeijuBannerService bannerService;
    private final AppWeijuDeliverService deliverService;
    private final AppCommonConfigService commonConfigService;
    private final AppUIConfigService    uiConfigService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelApp addNovelApp(NovelApp novelApp) {
        save(novelApp);
        return novelApp;
    }

    @Override
    public Map<String, List<NovelApp>> getNovelAppsByPlatform() {
        List<NovelApp> apps = list();
        return apps.stream()
                .collect(Collectors.groupingBy(NovelApp::getPlatform));
    }
    
    @Override
    public List<NovelApp> getAppsByAppName(String appName) {
        LambdaQueryWrapper<NovelApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelApp::getAppName, appName);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelApp updateNovelApp(NovelApp novelApp) {
        updateById(novelApp);
        return novelApp;
    }

    @Override
    public NovelApp getByAppId(String appId) {
        LambdaQueryWrapper<NovelApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelApp::getAppid, appId);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByAppId(String appId) {
        try {
            log.info("开始删除应用及其所有相关数据，appId: {}", appId);
            
            // 获取应用信息
            NovelApp existingApp = getByAppId(appId);
            if (existingApp == null) {
                log.warn("未找到对应的应用记录，appId: {}", appId);
                return false;
            }
            
            // 1. 删除通用配置
            commonConfigService.deleteAppCommonConfig(appId);
            log.info("已删除通用配置");
            
            // 2. 删除广告配置
            appAdService.deleteAppAdByAppId(appId);
            log.info("已删除广告配置");
            
            // 3. 删除支付配置
            appPayService.deleteAppPayByAppId(appId);
            log.info("已删除支付配置");
            
            // 4. 删除微距Banner
            if (existingApp.getBannerId() != null && !existingApp.getBannerId().isEmpty()) {
                bannerService.deleteBannerByBannerId(existingApp.getBannerId());
                log.info("已删除微距Banner");
            }
            
            // 5. 删除微距Deliver
            if (existingApp.getDeliverId() != null && !existingApp.getDeliverId().isEmpty()) {
                deliverService.deleteByDeliverId(existingApp.getDeliverId());
                log.info("已删除微距Deliver");
            }

            // 6. 删除UI配置
            uiConfigService.deleteAppUIConfigByAppId(appId);
            // 7. 最后删除应用本身
            boolean result = removeById(existingApp.getId());
            
            if (result) {
                log.info("应用删除成功，appId: {}", appId);
                return true;
            } else {
                log.warn("应用删除失败，appId: {}", appId);
                return false;
            }
        } catch (Exception e) {
            log.error("删除应用及其相关数据时发生错误，appId: {}, error: {}", appId, e.getMessage(), e);
            throw new RuntimeException("删除应用及其相关数据失败: " + e.getMessage());
        }
    }

}
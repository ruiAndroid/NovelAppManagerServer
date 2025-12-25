package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.ai.entity.FunAiApp;
import com.fun.novel.mapper.FunAiAppMapper;
import com.fun.novel.service.FunAiAppService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * AI应用服务实现类
 */
@Service
public class FunAiAppServiceImpl extends ServiceImpl<FunAiAppMapper, FunAiApp> implements FunAiAppService {

    @Override
    public List<FunAiApp> getAppsByUserId(Long userId) {
        QueryWrapper<FunAiApp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Page<FunAiApp> getAppsByUserId(Long userId, Integer page, Integer size) {
        Page<FunAiApp> appPage = new Page<>(page, size);
        QueryWrapper<FunAiApp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("create_time");
        return baseMapper.selectPage(appPage, queryWrapper);
    }

    @Override
    public FunAiApp getAppByIdAndUserId(Long appId, Long userId) {
        QueryWrapper<FunAiApp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", appId)
                .eq("user_id", userId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public FunAiApp createApp(FunAiApp app) {
        // 生成应用密钥
        if (!StringUtils.hasText(app.getAppKey())) {
            app.setAppKey(generateAppKey());
        }
        if (!StringUtils.hasText(app.getAppSecret())) {
            app.setAppSecret(generateAppSecret());
        }
        // 设置默认状态为启用
        if (app.getAppStatus() == null) {
            app.setAppStatus(1);
        }
        // 保存应用
        save(app);
        return app;
    }

    @Override
    public FunAiApp updateApp(FunAiApp app, Long userId) {
        // 验证应用是否属于该用户
        FunAiApp existingApp = getAppByIdAndUserId(app.getId(), userId);
        if (existingApp == null) {
            throw new RuntimeException("应用不存在或无权限操作");
        }
        // 更新应用信息
        app.setUserId(userId); // 确保用户ID不变
        updateById(app);
        return app;
    }

    @Override
    public boolean deleteApp(Long appId, Long userId) {
        // 验证应用是否属于该用户
        FunAiApp existingApp = getAppByIdAndUserId(appId, userId);
        if (existingApp == null) {
            throw new RuntimeException("应用不存在或无权限操作");
        }
        // 删除应用
        return removeById(appId);
    }

    /**
     * 生成应用密钥
     * @return 应用密钥
     */
    private String generateAppKey() {
        return "AIK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    /**
     * 生成应用密钥
     * @return 应用密钥
     */
    private String generateAppSecret() {
        return "AIS_" + UUID.randomUUID().toString().replace("-", "");
    }
}

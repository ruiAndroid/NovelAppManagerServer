package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.mapper.NovelAppMapper;
import com.fun.novel.service.NovelAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NovelAppServiceImpl implements NovelAppService {
    
    @Autowired
    private NovelAppMapper novelAppMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelApp addNovelApp(NovelApp novelApp) {
        if (checkAppIdExists(novelApp.getAppid())) {
            throw new IllegalArgumentException("应用ID已存在");
        }
        novelAppMapper.insert(novelApp);
        return novelApp;
    }

    /**
     * 检查应用ID是否已存在
     */
    private boolean checkAppIdExists(String appId) {
        LambdaQueryWrapper<NovelApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelApp::getAppid, appId);
        return novelAppMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Map<String, List<NovelApp>> getNovelAppsByPlatform() {
        // 获取所有小说应用
        List<NovelApp> allApps = novelAppMapper.selectList(null);
        
        // 按平台分组
        return allApps.stream()
                .collect(Collectors.groupingBy(
                        NovelApp::getPlatform,
                        Collectors.toList()
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByAppId(String appId) {
        // 先检查应用是否存在
        LambdaQueryWrapper<NovelApp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NovelApp::getAppid, appId);
        NovelApp existingApp = novelAppMapper.selectOne(queryWrapper);
        
        if (existingApp == null) {
            throw new IllegalArgumentException("应用不存在");
        }

        // 执行删除操作
        int rows = novelAppMapper.delete(queryWrapper);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelApp updateNovelApp(NovelApp novelApp) {
        if (novelApp == null || novelApp.getAppid() == null) {
            throw new IllegalArgumentException("应用信息不能为空");
        }

        // 检查应用是否存在
        NovelApp existingApp = getByAppId(novelApp.getAppid());
        if (existingApp == null) {
            throw new IllegalArgumentException("应用不存在");
        }
        // 设置ID
        novelApp.setId(existingApp.getId());
        
        // 执行更新
        novelAppMapper.updateById(novelApp);
        
        // 重新查询并返回最新数据
        return getByAppId(novelApp.getAppid());
    }

    private NovelApp getByAppId(String appId) {
        LambdaQueryWrapper<NovelApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelApp::getAppid, appId);
        return novelAppMapper.selectOne(wrapper);
    }
}
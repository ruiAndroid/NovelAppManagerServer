package com.fun.novel.service.impl;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppResourceFileService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NovelAppResourceFileServiceImpl implements NovelAppResourceFileService {
    @Override
    public void processAllResourceFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        // TODO: 实现资源文件相关的所有原子操作
    }
} 
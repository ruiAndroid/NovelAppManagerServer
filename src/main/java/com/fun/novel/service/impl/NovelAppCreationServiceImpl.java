package com.fun.novel.service.impl;

import com.fun.novel.dto.*;
import com.fun.novel.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NovelAppCreationServiceImpl implements NovelAppCreationService {

    @Autowired
    private NovelAppResourceFileService novelAppResourceFileService;
    @Autowired
    private NovelAppDatabaseOperationService novelAppDatabaseOperationService;
    @Autowired
    private NovelAppLocalFileOperationService novelAppLocalFileOperationService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNovelAppOperations(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        // 1.数据库操作
        novelAppDatabaseOperationService.processDatabaseOperations(taskId, params);
        // 2.本地代码文件操作
        novelAppLocalFileOperationService.createNovelAppLocalCodeFiles(taskId, params, rollbackActions);
        // 3.资源文件处理
        novelAppResourceFileService.createResourceFiles(taskId, params, rollbackActions);
    }
} 
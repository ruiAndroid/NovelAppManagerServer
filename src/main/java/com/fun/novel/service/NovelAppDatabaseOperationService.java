package com.fun.novel.service;

import com.fun.novel.dto.CreateNovelAppRequest;

public interface NovelAppDatabaseOperationService {
    /**
     * 处理所有数据库相关的原子操作，所有操作成功才提交，否则全部回滚
     * @param taskId 任务ID
     * @param params 创建参数
     */
    void processDatabaseOperations(String taskId, CreateNovelAppRequest params);
} 
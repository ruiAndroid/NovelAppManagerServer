package com.fun.novel.service;

import com.fun.novel.dto.CreateNovelAppRequest;
import java.util.List;

public interface NovelAppCreationService {

    /**
     * 创建小说小程序 数据库+本地文件的原子操作，所有操作成功才提交，否则全部回滚
     * @param taskId
     * @param params
     * @param rollbackActions
     */
    void createNovelAppOperations(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions);
} 
package com.fun.novel.service;

import com.fun.novel.dto.CreateNovelAppRequest;
import java.util.List;

public interface NovelAppCreationService {
    /**
     * 数据库+本地文件的原子操作，所有操作成功才提交，否则全部回滚
     * @param taskId 任务ID
     * @param params 创建参数
     * @param rollbackActions 文件操作的回滚动作列表
     */
    void processAllOperations(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions);
} 
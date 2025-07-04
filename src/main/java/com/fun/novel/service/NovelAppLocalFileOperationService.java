package com.fun.novel.service;

import com.fun.novel.dto.CreateNovelAppRequest;
import java.util.List;

public interface NovelAppLocalFileOperationService {
    /**
     * 处理所有本地代码文件相关的原子操作，所有操作成功才提交，否则全部回滚
     * @param taskId 任务ID
     * @param params 创建参数
     * @param rollbackActions 文件操作的回滚动作列表
     */
    void processLocalCodeFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions);
    /**
     * 更新所有基础配置相关的代码文件的原子操作，所有操作成功才提交，否则全部回滚
     * @param params
     * @param rollbackActions
     */
    void updateBaseConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions);  
} 
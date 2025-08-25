package com.fun.novel.service;

import com.fun.novel.dto.CreateNovelAppRequest;
import java.util.List;

public interface NovelAppLocalFileOperationService {


    /**
     * 创建所有本地代码文件相关的原子操作，所有操作成功才提交，否则全部回滚
     * @param taskId
     * @param params
     * @param rollbackActions
     */
    void createNovelAppLocalCodeFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions);



    void updateBaseConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions);

    void updateAdConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions);

    void updateCommonConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions);

    void updatePayConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions);

    void deleteAppLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions);
}
package com.fun.novel.service;

import com.fun.novel.entity.UserOpLog;

/**
 * 用户操作日志服务接口
 */
public interface UserOpLogService {
    
    /**
     * 保存操作日志
     * @param userOpLog 操作日志实体
     */
    void saveOpLog(UserOpLog userOpLog);
}
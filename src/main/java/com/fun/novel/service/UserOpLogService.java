package com.fun.novel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fun.novel.entity.UserOpLog;

import java.util.List;

/**
 * 用户操作日志服务接口
 */
public interface UserOpLogService {
    
    /**
     * 保存操作日志
     * @param userOpLog 操作日志实体
     */
    void saveOpLog(UserOpLog userOpLog);
    
    /**
     * 根据用户ID查询所有操作日志记录
     * @param userId 用户ID
     * @return 操作日志列表
     */
    List<UserOpLog> queryUserAllOp(Long userId);
    
    /**
     * 根据用户ID分页查询操作日志记录
     * @param userId 用户ID
     * @param page 分页参数
     * @return 分页结果
     */
    IPage<UserOpLog> queryUserAllOpWithPage(Long userId, Page<UserOpLog> page);
    
    /**
     * 分页查询所有操作日志记录（默认按时间倒序）
     * @param page 分页参数
     * @return 分页结果
     */
    IPage<UserOpLog> queryAllOpWithPage(Page<UserOpLog> page);
}
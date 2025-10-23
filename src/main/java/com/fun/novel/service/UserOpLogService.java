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
     * 查询所有操作日志记录（默认按时间倒序）
     * @return 操作日志列表
     */
    List<UserOpLog> queryAllOp();
    

    /**
     * 根据查询条件分页查询所有操作日志记录
     * @param query 查询条件
     * @param page 分页参数
     * @return 分页结果
     */
    IPage<UserOpLog> queryAllOpWithPageAndQuery(String query, Page<UserOpLog> page);
}
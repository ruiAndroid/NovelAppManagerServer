package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.UserOpLog;
import com.fun.novel.mapper.UserOpLogMapper;
import com.fun.novel.service.UserOpLogService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 用户操作日志服务实现类
 */
@Service
public class UserOpLogServiceImpl extends ServiceImpl<UserOpLogMapper, UserOpLog> implements UserOpLogService {
    
    @Override
    public void saveOpLog(UserOpLog userOpLog) {
        this.save(userOpLog);
    }
    

    
    @Override
    public List<UserOpLog> queryAllOp() {
        return this.lambdaQuery()
                .orderByDesc(UserOpLog::getUpdateTime)
                .list();
    }
    @Override
    public IPage<UserOpLog> queryAllOpWithPageAndQuery(String query, Page<UserOpLog> page) {
        if (query == null || query.trim().isEmpty()) {
            // 如果查询条件为空，则返回所有记录
            return this.lambdaQuery()
                    .orderByDesc(UserOpLog::getUpdateTime)
                    .page(page);
        }
        
        // 根据查询条件进行模糊匹配
        return this.lambdaQuery()
                .and(wrapper -> wrapper
                    .like(UserOpLog::getUserName, query)
                    .or()
                    .like(UserOpLog::getMethodName, query)
                    .or()
                    .like(UserOpLog::getRequestUrl, query)
                    .or()
                    .like(UserOpLog::getRequestParams, query)
                    .or()
                    .like(UserOpLog::getResponseResult, query)
                    .or()
                    .like(UserOpLog::getOpName, query))
                .orderByDesc(UserOpLog::getUpdateTime)
                .page(page);
    }
}
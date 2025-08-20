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
    public List<UserOpLog> queryUserAllOp(Long userId) {
        return this.lambdaQuery()
                .eq(UserOpLog::getUserId, userId)
                .orderByDesc(UserOpLog::getUpdateTime)
                .list();
    }
    
    @Override
    public IPage<UserOpLog> queryUserAllOpWithPage(Long userId, Page<UserOpLog> page) {
        return this.lambdaQuery()
                .eq(UserOpLog::getUserId, userId)
                .orderByDesc(UserOpLog::getUpdateTime)
                .page(page);
    }
}
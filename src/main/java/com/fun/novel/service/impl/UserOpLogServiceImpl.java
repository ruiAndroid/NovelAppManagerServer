package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.UserOpLog;
import com.fun.novel.mapper.UserOpLogMapper;
import com.fun.novel.service.UserOpLogService;
import org.springframework.stereotype.Service;

/**
 * 用户操作日志服务实现类
 */
@Service
public class UserOpLogServiceImpl extends ServiceImpl<UserOpLogMapper, UserOpLog> implements UserOpLogService {
    
    @Override
    public void saveOpLog(UserOpLog userOpLog) {
        this.save(userOpLog);
    }
}
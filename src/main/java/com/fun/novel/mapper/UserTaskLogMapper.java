package com.fun.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fun.novel.entity.UserTaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户任务日志 Mapper 接口
 */
@Mapper
public interface UserTaskLogMapper extends BaseMapper<UserTaskLog> {
}
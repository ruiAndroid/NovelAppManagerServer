package com.fun.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fun.novel.entity.UserOpLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户操作日志 Mapper 接口
 */
@Mapper
public interface UserOpLogMapper extends BaseMapper<UserOpLog> {
}
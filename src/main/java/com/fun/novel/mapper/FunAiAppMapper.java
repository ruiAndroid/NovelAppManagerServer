package com.fun.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fun.novel.ai.entity.FunAiApp;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI应用Mapper接口
 */
@Mapper
public interface FunAiAppMapper extends BaseMapper<FunAiApp> {
    // 可以添加自定义的查询方法
}

package com.fun.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fun.novel.entity.AppCommonConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppCommonConfigMapper extends BaseMapper<AppCommonConfig> {
    
    /**
     * 根据应用名称查询通用配置列表
     * @param appName 应用名称
     * @return 通用配置列表
     */
    @Select("SELECT acc.* FROM app_common_config acc " +
            "JOIN novel_app na ON acc.appid = na.appid " +
            "WHERE na.app_name = #{appName}")
    List<AppCommonConfig> selectByAppName(String appName);
}
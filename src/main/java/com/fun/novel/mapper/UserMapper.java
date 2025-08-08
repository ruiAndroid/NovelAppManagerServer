package com.fun.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fun.novel.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
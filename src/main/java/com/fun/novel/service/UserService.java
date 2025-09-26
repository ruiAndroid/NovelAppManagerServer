package com.fun.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fun.novel.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户实体
     */
    User findByUsername(String username);

    /**
     * 注册用户
     * @param user 用户实体
     * @return 注册后的用户实体
     */
    User register(User user);
    
    /**
     * 根据用户名获取用户ID
     * @param username 用户名
     * @return 用户ID
     */
    Long getUserIdByUsername(String username);
    
    /**
     * 分页查询用户信息
     * @param page 页码
     * @param size 每页条数
     * @return 用户分页信息
     */
    Page<User> getUserPage(Integer page, Integer size);
    
    /**
     * 分页查询用户信息（支持关键词搜索）
     * @param page 页码
     * @param size 每页条数
     * @param keyword 搜索关键词，可以是用户ID或用户名
     * @return 用户分页信息
     */
    Page<User> getUserPage(Integer page, Integer size, String keyword);
}
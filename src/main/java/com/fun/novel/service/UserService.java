package com.fun.novel.service;

import com.fun.novel.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
package com.fun.novel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fun.novel.ai.entity.FunAiUser;

/**
 * 用户服务接口
 */
public interface FunAiUserService extends IService<FunAiUser> {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户实体
     */
    FunAiUser findByUsername(String username);

    /**
     * 注册用户
     * @param user 用户实体
     * @return 注册后的用户实体
     */
    FunAiUser register(FunAiUser user);
    
    /**
     * 根据用户名获取用户ID
     * @param username 用户名
     * @return 用户ID
     */
    Long getUserIdByUsername(String username);
    

}
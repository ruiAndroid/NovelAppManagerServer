package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fun.novel.entity.User;
import com.fun.novel.mapper.UserMapper;
import com.fun.novel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", username); // 注意这里使用的是user_name而不是username
        return getBaseMapper().selectOne(queryWrapper);
    }

    @Override
    public User register(User user) {
        // 检查用户名是否已存在
        if (findByUsername(user.getUserName()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setType(user.getType());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());


        // 使用MyBatis Plus的save方法确保ID自动生成
        boolean saved = save(user);
        if (!saved) {
            throw new RuntimeException("用户注册失败");
        }
        return user;
    }

    @Override
    public Long getUserIdByUsername(String username) {
        User user = findByUsername(username);
        return user != null ? user.getId() : null;
    }
}
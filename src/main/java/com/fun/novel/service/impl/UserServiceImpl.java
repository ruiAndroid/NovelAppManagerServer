package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 确保状态字段被正确设置
        if (user.getStatus() == null) {
            // 如果状态未设置，根据用户类型设置默认状态
            if (user.getType() != null) {
                if (user.getType() == 0) {
                    // 研发人员默认审核通过
                    user.setStatus(0);
                } else if (user.getType() == 1 || user.getType() == 2) {
                    // 产品或测试人员默认待审核
                    user.setStatus(1);
                } else {
                    // 其他类型默认待审核
                    user.setStatus(1);
                }
            } else {
                // 默认待审核
                user.setStatus(1);
            }
        }

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
    
    @Override
    public Page<User> getUserPage(Integer page, Integer size) {
        // 创建分页对象
        Page<User> userPage = new Page<>(page, size);
        
        // 创建查询条件，按更新时间降序排列
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("update_time");
        
        // 执行分页查询
        return page(userPage, queryWrapper);
    }
    
    @Override
    public Page<User> getUserPage(Integer page, Integer size, String keyword) {
        // 创建分页对象
        Page<User> userPage = new Page<>(page, size);
        
        // 创建查询条件，按更新时间降序排列
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("update_time");
        
        // 如果提供了关键词，则添加搜索条件
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 尝试将关键词解析为ID
            try {
                Long userId = Long.parseLong(keyword.trim());
                // 使用or条件搜索ID或用户名
                queryWrapper.and(wrapper -> wrapper.eq("id", userId).or().like("user_name", keyword.trim()));
            } catch (NumberFormatException e) {
                // 如果不是数字，则只按用户名搜索
                queryWrapper.like("user_name", keyword.trim());
            }
        }
        
        // 执行分页查询
        return page(userPage, queryWrapper);
    }

}
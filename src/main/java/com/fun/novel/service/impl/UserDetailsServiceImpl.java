package com.fun.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fun.novel.ai.entity.FunAiUser;
import com.fun.novel.entity.User;
import com.fun.novel.mapper.FunAiUserMapper;
import com.fun.novel.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    private final UserMapper userMapper;
    private final FunAiUserMapper funAiUserMapper;
    public UserDetailsServiceImpl(UserMapper userMapper, FunAiUserMapper funAiUserMapper) {
        this.userMapper = userMapper;
        this.funAiUserMapper = funAiUserMapper;
    }


    public UserDetails loadFunAiUserByUsername(String username) throws UsernameNotFoundException{
        QueryWrapper<FunAiUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", username);
        FunAiUser user = funAiUserMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserName())
                .password(user.getPassword())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查找用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 根据用户类型分配权限
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getType() == 0 ) {
            // type=0研发或type=1产品 拥有所有权限
            authorities.add(new SimpleGrantedAuthority("ROLE_0"));
        } else if (user.getType() == 1) {
            // type=2 或 type=3 只能访问特定接口
            authorities.add(new SimpleGrantedAuthority("ROLE_1"));
        } else if(user.getType() == 2){
            authorities.add(new SimpleGrantedAuthority("ROLE_2"));
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserName())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
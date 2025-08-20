package com.fun.novel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fun.novel.common.Result;
import com.fun.novel.entity.User;
import com.fun.novel.service.UserService;
import com.fun.novel.utils.JwtUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;


@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    private final ObjectMapper objectMapper;

    private final UserService userService;

    public LoginSuccessHandler(JwtUtil jwtUtil, ObjectMapper objectMapper, @Lazy UserService userService) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        ServletOutputStream outputStream = response.getOutputStream();
        User user = (User) authentication.getPrincipal();
        // 生成jwt，并放置到响应头中
        String token = jwtUtil.generateToken(user.getUserName());
        response.setHeader("Authorization", "Bearer " + token);
        
        // 更新用户最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);
        
        // 返回用户信息
        Result<User> result = Result.success(user);
        
        // 手动调用jackson序列化
        outputStream.write(objectMapper.writeValueAsString(result).getBytes("UTF-8"));

        outputStream.flush();
        outputStream.close();
    }
}
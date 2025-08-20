package com.fun.novel.controller;

import com.fun.novel.common.Const;
import com.fun.novel.common.Result;
import com.fun.novel.dto.LoginRequest;
import com.fun.novel.dto.RegisterRequest;
import com.fun.novel.entity.User;
import com.fun.novel.service.UserService;
import com.fun.novel.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/novel-auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录并获取JWT Token")
    public Result<User> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUserName(),
                        loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = userService.findByUsername(loginRequest.getUserName());
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(loginRequest.getUserName());
            logger.info("Generated JWT Token for user {}: {}", loginRequest.getUserName(), token);
            
            // 设置Authorization响应头
            response.setHeader("Authorization", "Bearer " + token);
            
            // 更新上次登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userService.updateById(user);
            
            return Result.success(user);
        } catch (Exception e) {
            logger.error("Login failed for user: " + loginRequest.getUserName(), e);
            return Result.error("用户名或密码错误");
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public Result<User> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // 检查用户是否已存在
            if (userService.findByUsername(registerRequest.getUserName()) != null) {
                return Result.error("用户名已存在");
            }

            // 创建用户对象
            User user = new User();
            user.setUserName(registerRequest.getUserName());
            user.setPassword(registerRequest.getPassword());
            user.setPhone(registerRequest.getPhone());
            user.setAvatar(Const.USER_DEFAULT_AVATAR);
            user.setType(registerRequest.getType());

            // 注册用户
            User registeredUser = userService.register(user);

            // 生成JWT Token
//            String token = jwtUtil.generateToken(registeredUser.getUserName());

            return Result.success("注册成功", registeredUser);
        } catch (Exception e) {
            return Result.error("注册失败: " + e.getMessage());
        }
    }
}
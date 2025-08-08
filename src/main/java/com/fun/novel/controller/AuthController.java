package com.fun.novel.controller;

import com.fun.novel.common.Const;
import com.fun.novel.common.Result;
import com.fun.novel.dto.LoginResponse;
import com.fun.novel.dto.LoginRequest;
import com.fun.novel.dto.RegisterRequest;
import com.fun.novel.entity.User;
import com.fun.novel.service.UserService;
import com.fun.novel.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/novel-auth")
public class AuthController {

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
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUserName(),
                        loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User byUsername = userService.findByUsername(loginRequest.getUserName());
            // 生成JWT Token
            String token = jwtUtil.generateToken(loginRequest.getUserName());
            //更新上次登录时间
            byUsername.setLastLoginTime(LocalDateTime.now());
            userService.updateById(byUsername);
            return Result.success(new LoginResponse(token, byUsername));
        } catch (Exception e) {
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
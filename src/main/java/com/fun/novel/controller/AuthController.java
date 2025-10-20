package com.fun.novel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fun.novel.annotation.OperationLog;
import com.fun.novel.common.Const;
import com.fun.novel.common.Result;
import com.fun.novel.dto.LoginRequest;
import com.fun.novel.dto.RegisterRequest;
import com.fun.novel.entity.User;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.UserService;
import com.fun.novel.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.LocalDateTime;

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
            // 先根据用户名查找用户，检查用户状态
            User user = userService.findByUsername(loginRequest.getUserName());
            if (user == null) {
                return Result.error("用户名或密码错误");
            }

            // 检查用户状态
            if (user.getStatus() != null) {
                switch (user.getStatus()) {
                    case 1: // 待审核
                        return Result.error("账号正在审核中，请耐心等待");
                    case 2: // 审核失败
                        return Result.error("账号审核未通过，请联系管理员");
                    case 0: // 审核通过，继续登录流程
                        break;
                    default:
                        // 其他状态默认允许登录
                        break;
                }
            }

            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUserName(),
                        loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(loginRequest.getUserName());
            logger.info("Generated JWT Token for user {}: length={}", loginRequest.getUserName(), token.length());
            
            // 设置Authorization响应头
            response.setHeader("Authorization", "Bearer " + token);
            response.setHeader("Access-Control-Expose-Headers", "Authorization");
            
            // 更新上次登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userService.updateById(user);
            
            logger.info("Login successful for user: {}", loginRequest.getUserName());
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
            user.setEmail(registerRequest.getEmail());
            user.setAvatar(Const.USER_DEFAULT_AVATAR);
            user.setType(registerRequest.getType());
            
            // 根据用户类型设置状态
            // 用户类型: 0研发，1产品，2测试
            // 用户状态: 0审核通过，1待审核，2审核失败

            if (registerRequest.getType() != null) {
                if (registerRequest.getType() == 0) {
                    // 研发人员默认审核通过
                    user.setStatus(0);
                } else if (registerRequest.getType() == 1 || registerRequest.getType() == 2) {
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

            // 注册用户
            User registeredUser = userService.register(user);
            
            // 根据用户状态返回不同提示信息
            String message;
            if (registeredUser.getStatus() != null) {
                switch (registeredUser.getStatus()) {
                    case 0:
                        message = "注册成功";
                        break;
                    case 1:
                        message = "注册成功，账号待审核中";
                        break;
                    case 2:
                        message = "注册成功，账号审核未通过";
                        break;
                    default:
                        message = "注册成功";
                        break;
                }
            } else {
                message = "注册成功";
            }

            return Result.success(message, registeredUser);
        } catch (Exception e) {
            return Result.error("注册失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/users")
    @Operation(summary = "分页查询用户信息", description = "分页查询所有用户信息，按更新时间排序")
    @PreAuthorize("hasAnyRole('ROLE_0')")
    @OperationLog(opType = OpType.QUERY_CODE, opName = "分页查询用户信息", recordParams = false)
    public Result<Page<User>> getUserPage(
            @Parameter(description = "页码，默认为1") 
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "每页条数，默认为10") 
            @RequestParam(defaultValue = "10") Integer size,
            
            @Parameter(description = "搜索关键词，可以是用户ID或用户名") 
            @RequestParam(required = false) String keyword) {
        
        try {
            Page<User> userPage = userService.getUserPage(page, size, keyword);
            return Result.success("查询成功", userPage);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/users/approve/{userId}")
    @Operation(summary = "审核通过用户", description = "将待审核的用户状态改为审核通过状态")
    @PreAuthorize("hasAnyRole('ROLE_0')")
    @OperationLog(opType = OpType.UPDATE_CODE, opName = "将待审核的用户状态改为审核通过状态")
    public Result<User> approveUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 检查用户当前状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                return Result.error("只有待审核状态的用户才能进行审核通过操作");
            }
            
            // 更新用户状态为审核通过
            user.setStatus(0);
            user.setUpdateTime(LocalDateTime.now());
            boolean updated = userService.updateById(user);
            
            if (updated) {
                return Result.success("用户审核通过", user);
            } else {
                return Result.error("审核通过操作失败");
            }
        } catch (Exception e) {
            return Result.error("审核通过操作失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/users/delete/{userId}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户")
    @PreAuthorize("hasAnyRole('ROLE_0')")
    @OperationLog(opType = OpType.DELETE_CODE, opName = "根据用户ID删除用户")
    public Result<String> deleteUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 获取当前认证用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                // 检查是否是当前用户自己删除自己
                if (currentUser.getId().equals(userId)) {
                    return Result.error("不能删除自己的账户");
                }
            }
            
            boolean removed = userService.removeById(userId);
            
            if (removed) {
                return Result.success("用户删除成功");
            } else {
                return Result.error("用户删除失败");
            }
        } catch (Exception e) {
            return Result.error("用户删除失败: " + e.getMessage());
        }
    }
}
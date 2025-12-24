package com.fun.novel.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fun.novel.common.Result;
import com.fun.novel.entity.FunAiUser;
import com.fun.novel.entity.User;
import com.fun.novel.mapper.FunAiUserMapper;
import com.fun.novel.service.impl.UserDetailsServiceImpl;
import com.fun.novel.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class DynamicJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DynamicJwtAuthenticationFilter.class);

    private final UserDetailsService userDetailsService;
    private final FunAiUserMapper funAiUserMapper;
    private final JwtUtil jwtUtil;

    public DynamicJwtAuthenticationFilter(
            @Qualifier("userDetailsServiceImpl") UserDetailsService userDetailsService,
            FunAiUserMapper funAiUserMapper,
            JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.funAiUserMapper = funAiUserMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");
        String url = request.getServletPath();

        String username = null;
        String jwtToken = null;

        // JWT Token的格式为 "Bearer token"
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.warn("Unable to get JWT Token: {}", e.getMessage());
                sendErrorResponse(response, "请求中没有JWT Token");
                return;
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token已过期: {}", e.getMessage());
                sendErrorResponse(response, "JWT Token已过期");
                return;
            } catch (SignatureException e) {
                logger.warn("JWT签名不匹配: {}", e.getMessage());
                sendErrorResponse(response, "JWT签名无效");
                return;
            } catch (MalformedJwtException e) {
                logger.warn("JWT格式错误: {}", e.getMessage());
                sendErrorResponse(response, "JWT格式无效");
                return;
            } catch (UnsupportedJwtException e) {
                logger.warn("不支持的JWT Token: {}", e.getMessage());
                sendErrorResponse(response, "不支持的JWT Token");
                return;
            } catch (Exception e) {
                logger.warn("JWT Token处理异常: {}", e.getMessage());
                sendErrorResponse(response, "JWT Token无效");
                return;
            }

            // 验证token
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails;
                    
                    // 根据URL路径决定使用哪个用户详情服务
                    if (url.contains("fun-ai")) {
                        // 使用FunAiUserMapper直接查询用户
                        QueryWrapper<FunAiUser> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("user_name", username);
                        FunAiUser funAiUser = funAiUserMapper.selectOne(queryWrapper);
                        
                        if (funAiUser != null && jwtUtil.validateToken(jwtToken, funAiUser.getUserName())) {
                            userDetails = org.springframework.security.core.userdetails.User.builder()
                                    .username(funAiUser.getUserName())
                                    .password(funAiUser.getPassword())
                                    .accountExpired(false)
                                    .accountLocked(false)
                                    .credentialsExpired(false)
                                    .disabled(false)
                                    .build();
                        } else {
                            logger.warn("FunAI用户JWT Token验证失败");
                            sendErrorResponse(response, "JWT Token无效");
                            return;
                        }
                    } else {
                        // 使用默认的UserDetailsService加载用户
                        userDetails = this.userDetailsService.loadUserByUsername(username);
                        if (!jwtUtil.validateToken(jwtToken, userDetails.getUsername())) {
                            logger.warn("普通用户JWT Token验证失败");
                            sendErrorResponse(response, "JWT Token无效");
                            return;
                        }
                    }
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                } catch (Exception e) {
                    logger.warn("用户认证失败: {}", e.getMessage());
                    sendErrorResponse(response, "用户认证失败");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        
        Result<Object> errorResult = Result.error(401, message);
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResult));
    }
}
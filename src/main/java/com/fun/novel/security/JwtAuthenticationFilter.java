package com.fun.novel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fun.novel.common.Result;
import com.fun.novel.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final UserDetailsService userDetailsService;

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(UserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");

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
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    if (jwtUtil.validateToken(jwtToken, userDetails.getUsername())) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        logger.warn("JWT Token验证失败");
                        sendErrorResponse(response, "JWT Token无效");
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("用户认证失败: {}", e.getMessage());
                    sendErrorResponse(response, "用户认证失败");
                    return;
                }
            }
        } 
        // 如果没有Authorization头或者不是Bearer Token格式，则直接放行
        // 让Spring Security的其他配置来决定是否需要认证
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
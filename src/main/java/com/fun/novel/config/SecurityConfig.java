package com.fun.novel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fun.novel.common.Result;
import com.fun.novel.mapper.FunAiUserMapper;
import com.fun.novel.security.DynamicJwtAuthenticationFilter;
import com.fun.novel.security.JwtAuthenticationFilter;
import com.fun.novel.service.FunAiUserService;
import com.fun.novel.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final UserDetailsService userDetailsService;
    private final FunAiUserMapper funAiUserMapper;
    private final JwtUtil jwtUtil;

    public SecurityConfig(@Lazy UserDetailsService userDetailsService, FunAiUserMapper funAiUserMapper, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.funAiUserMapper = funAiUserMapper;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            // 禁用表单登录
            .formLogin().disable()
            // 禁用HTTP基本认证
            .httpBasic().disable()

            // 禁用session
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // 配置认证入口点，处理未认证的请求
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint())

             // 配置拦截规则
            .and()
            .authorizeHttpRequests()
            .requestMatchers(URL_WHITELIST).permitAll() // 接口请求白名单
//            .requestMatchers("/api/novel-apps/**").hasAnyRole("USER_TYPE_2_3", "ADMIN") // type=2或3可以访问的接口
            .anyRequest().authenticated() // 其他请求需要认证

             //配置自定义的过滤器
            .and()
            .addFilterBefore(dynamicJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            
            Result<Object> result = Result.error(401, "请先登录");
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(result));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setHideUserNotFoundExceptions(false);
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public DynamicJwtAuthenticationFilter dynamicJwtAuthenticationFilter() {
        return new DynamicJwtAuthenticationFilter(userDetailsService, funAiUserMapper, jwtUtil);
    }
    
    //白名单接口，不需要鉴权可以直接调用的
    public static final String[] URL_WHITELIST = {
        "/swagger-ui.html",//接口文档
        "/swagger-ui/**",
        "/chatui/**",
        "/api/ai/chat/**",
        "/v3/api-docs/**",
        "/webjars/**",
        "/api/novel-auth/login",  //登录
        "/api/novel-auth/register",//注册
        "/api/novel-auth/fun-ai-login",  //风行AI登录
        "/api/novel-auth/fun-ai-register",//风行AI注册
        "/api/novel-apps/appLists",   //应用列表
        "/api/novel-weiju/banner/getBannerByBannerId",     //微距相关
        "/api/novel-weiju/deliver/getDeliverByDeliverId",
        "/api/novel-ad/appAd/getAppAdByAppId",//广告相关
        "/api/novel-pay/getAppPayByAppId",//支付相关
        "/api/novel-common/getAppCommonConfig",//通用配置相关
        "/api/novel-publish/list",//自动化相关
        "/ws/**", //WebSocket端点
        "/api/op-log/**" //WebSocket端点

    };
}
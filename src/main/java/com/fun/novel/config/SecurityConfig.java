package com.fun.novel.config;

import com.fun.novel.security.JwtAuthenticationFilter;
import com.fun.novel.security.LoginSuccessHandler;
import com.fun.novel.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final UserDetailsService userDetailsService;

    private final JwtUtil jwtUtil;

    private final LoginSuccessHandler loginSuccessHandler;

    //白名单接口，不需要鉴权可以直接调用的
    public static final String[] URL_WHITELIST = {
        "/swagger-ui.html",//接口文档
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/webjars/**",
        "/api/novel-auth/login",  //登录
        "/api/novel-auth/register",//注册
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

    public SecurityConfig(@Lazy UserDetailsService userDetailsService, JwtUtil jwtUtil, @Lazy LoginSuccessHandler loginSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            // 登录配置
            .formLogin()
            .successHandler(loginSuccessHandler)

            // 禁用session
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

             // 配置拦截规则
            .and()
            .authorizeRequests()
            .antMatchers(URL_WHITELIST).permitAll() // 接口请求白名单
//            .antMatchers("/api/novel-apps/**").hasAnyRole("USER_TYPE_2_3", "ADMIN") // type=2或3可以访问的接口
            .anyRequest().authenticated() // 其他请求需要认证
             //配置自定义的过滤器
            .and()
            .addFilterBefore(new JwtAuthenticationFilter(userDetailsService, jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
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
}
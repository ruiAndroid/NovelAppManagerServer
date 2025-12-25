package com.fun.novel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("文曲")
                        .description("文曲接口文档")
                        .version("1.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
    
    /**
     * 配置 Swagger UI 路径重定向
     * 确保访问 /swagger-ui/ 时能正确重定向到 /swagger-ui/index.html
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将 /swagger-ui/ 重定向到 /swagger-ui/index.html
        registry.addRedirectViewController("/swagger-ui/", "/swagger-ui/index.html");
        // 兼容旧路径
        registry.addRedirectViewController("/swagger-ui.html", "/swagger-ui/index.html");
    }
}
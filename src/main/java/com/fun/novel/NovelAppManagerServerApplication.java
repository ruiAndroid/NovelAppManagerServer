package com.fun.novel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class NovelAppManagerServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovelAppManagerServerApplication.class, args);
    }
}
package com.fun.novel.ai.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * fun ai chat相关控制器
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "funAiStudio 用户相关接口", description = "funAiStudio用户相关接口")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://172.17.5.80:5173",
        "http://172.17.5.80:8080"
}, allowCredentials = "true")
public class FunAiUserController {


}

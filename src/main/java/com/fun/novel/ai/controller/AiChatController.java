package com.fun.novel.ai.controller;

import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.fun.novel.ai.services.AiBaseService;
import com.fun.novel.ai.services.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ai chat相关控制器
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "ai chat相关接口", description = "ai chat相关接口")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://172.17.5.80:5173",
        "http://172.17.5.80:8080"
}, allowCredentials = "true")
public class AiChatController {
    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private AiBaseService aiBaseService;


    /**
     * 当发送的提示词(prompt)为空时，接口会返回错误信息
     * @param prompt
     * @param model
     * @param chatId
     * @return
     */
    @PostMapping(path="/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "DashScope Flux Chat")
    public Flux<String> chat(
            @Validated @RequestBody String prompt,
            @RequestHeader(value = "model", required = false) String model,
            @RequestHeader(value = "chatId", required = false, defaultValue = "default-chat-id") String chatId
    ) {
        Set<Map<String, String>> dashScope = aiBaseService.getDashScope();
        List<String> modelName = dashScope.stream()
                .flatMap(map -> map.keySet().stream().map(map::get))
                .distinct()
                .toList();

        if (StringUtils.hasText(model)) {
            if (!modelName.contains(model)) {
                return Flux.just("Input model not support.");
            }
        }
        else {
            model = DashScopeModel.ChatModel.QWEN_PLUS.getValue();
        }

        return aiChatService.chat(chatId, model, prompt);

    }

}
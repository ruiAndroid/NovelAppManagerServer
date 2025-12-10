package com.fun.novel.ai.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 内容安全检查拦截器
 */
public class GuardrailInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 前置：检查输入
        if (containsSensitiveContent(request.getMessages())) {
            return ModelResponse.of(new AssistantMessage("检测到非法内容"));
        }
        // 执行调用
        ModelResponse response = handler.call(request);
        // 后置：检查输出
        return sanitizeIfNeeded(response);
    }


    private boolean containsSensitiveContent(List<Message> messages) {
        // 实现敏感内容检测逻辑
        return false;
    }

    private ModelResponse sanitizeIfNeeded(ModelResponse response) {
        // 实现响应清理逻辑
        return response;
    }
    @Override
    public String getName() {
        return "GuardrailInterceptor";
    }
}

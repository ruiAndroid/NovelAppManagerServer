package com.fun.novel.ai.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;


/**
 * 专门针对 deepseek-r1 模型设计的顾问组件
 *
 * 推理内容提取: 从模型响应的元数据中提取 reasoningContent 字段
 * 内容包装: 将推理内容包装在 `
 */
public class ReasoningContentAdvisor implements BaseAdvisor {

    private final int order;

    public ReasoningContentAdvisor(Integer order) {
        this.order = order != null ? order : 0;
    }
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse resp = chatClientResponse.chatResponse();

        if (Objects.isNull(resp)) {
            return chatClientResponse;
        }

        // 注释，避免冗长的日志打印
        // logger.debug("Advisor metadata output: {}", resp.getResults().get(0).getOutput().getMetadata());
        String reasoningContent = String.valueOf(resp.getResults().get(0).getOutput().getMetadata().get("reasoningContent"));
        // logger.debug("Advisor reasoning content: {}", reasoningContent);
        if (StringUtils.hasText(reasoningContent)) {
            List<Generation> thinkGenerations = resp.getResults().stream()
                    .map(generation -> {
                        AssistantMessage output = generation.getOutput();
                        AssistantMessage thinkAssistantMessage = AssistantMessage.builder()
                                .content(String.format("<think>%s</think>", reasoningContent) + output.getText())
                                .properties(output.getMetadata())
                                .toolCalls(output.getToolCalls())
                                .media(output.getMedia())
                                .build();
                        return new Generation(thinkAssistantMessage, generation.getMetadata());
                    }).toList();

            ChatResponse thinkChatResp = ChatResponse.builder().from(resp).generations(thinkGenerations).build();
            return ChatClientResponse.builder().context(chatClientResponse.context()).chatResponse(thinkChatResp).build();

        }

        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}

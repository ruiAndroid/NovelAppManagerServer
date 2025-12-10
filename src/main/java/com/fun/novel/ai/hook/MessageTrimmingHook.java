package com.fun.novel.ai.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * ModelHook
 * 在模型调用前后执行（例如：消息修剪），
 * 区别于AgentHook，ModelHook在一次agent调用中可能会调用多次，
 * 也就是每次 reasoning-acting 迭代都会执行
 * Agent Loop 循环过程中，每次模型调用前后
 */
public class MessageTrimmingHook extends ModelHook {

    private static final int MAX_MESSAGES = 10;

    @Override
    public String getName() {
        return "message_trimming";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL};
    }

    /**
     * 限制模型调用次数
     */
    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        System.out.println("MessageTrimmingHook beforeModel:"+state);
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isPresent()) {
            List<Message> messages = (List<Message>) messagesOpt.get();
            System.out.println("MessageTrimmingHook beforeModel messages: " + messages);
            if (messages.size() > MAX_MESSAGES) {
                return CompletableFuture.completedFuture(Map.of("messages",
                        messages.subList(messages.size() - MAX_MESSAGES, messages.size())));
            }
        }
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }
}

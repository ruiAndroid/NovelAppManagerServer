package com.fun.novel.ai.hook;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * AgentHook
 * 在 Agent 开始/结束时执行，每次Agent调用只会运行一次
 */
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class LoggingHook extends AgentHook {

    @Override
    public String getName() {
        return "logging";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        System.out.println("beforeAgent Agent 开始执行");
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        System.out.println("afterAgent Agent 执行完成");
        return CompletableFuture.completedFuture(Map.of());
    }


}

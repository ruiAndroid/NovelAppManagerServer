package com.fun.novel.ai;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.fun.novel.ai.hook.LoggingHook;
import com.fun.novel.ai.hook.MessageTrimmingHook;
import com.fun.novel.ai.interceptor.DynamicPromptInterceptor;
import com.fun.novel.ai.interceptor.GuardrailInterceptor;
import com.fun.novel.ai.prompt.WeatherPrompt;
import com.fun.novel.ai.interceptor.ToolErrorInterceptor;
import com.fun.novel.ai.tools.UserLocationTool;
import com.fun.novel.ai.tools.WeatherForLocationTool;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AiTest {

    public static void main(String[] args) throws Exception {
        // 初始化 ChatModel
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey("sk-8e78660ba8034d528093e4fd7839f7df")
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-plus")  // 指定模型名称
                        .withTemperature(0.3)    // 降低温度使输出更稳定
                        .withMaxToken(1000)
                        .build())
                .build();

        // 创建工具列表
        List<ToolCallback> tools = new ArrayList<>();
        tools.add(new WeatherForLocationTool());
        tools.add(new UserLocationTool());

        //拦截器用途
        //ModelInterceptor：内容安全、动态提示、日志记录、性能监控
        //ToolInterceptor：错误重试、权限检查、结果缓存、审计日志

        // 创建 agent
        ReactAgent agent = ReactAgent.builder()
                .name("weather_agent")
                .model(chatModel)
                .tools(tools)  // 使用工具列表而不是单个工具
                .systemPrompt(WeatherPrompt.Weather_SYSTEM_PROMPT)
                //添加记忆
                .saver(new MemorySaver())
                .interceptors(
                        new ToolErrorInterceptor(),
                        new DynamicPromptInterceptor(),
                        new GuardrailInterceptor())
                .hooks(
                        new LoggingHook(),
                        new MessageTrimmingHook())
                .build();

        // threadId 是给定对话的唯一标识符
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId("test1")
                .addMetadata("user_id", "1")
                .build();

        // 运行 agent
        AssistantMessage result = agent.call("我所在城市的天气咋样?", runnableConfig);
        System.out.println(result.getText());

//        Optional<OverAllState> result = agent.invoke("我所在城市的天气咋样?", runnableConfig);
//        if (result.isPresent()) {
//            OverAllState state = result.get();
//            // 访问消息历史
//            Optional<Object> messages = state.value("messages");
//            List<Message> messageList = (List<Message>) messages.get();
//
//            // 访问自定义状态
//            Optional<Object> customData = state.value("custom_key");
//
//            System.out.println("messageList：" + messageList);
//
//        }



    }
}
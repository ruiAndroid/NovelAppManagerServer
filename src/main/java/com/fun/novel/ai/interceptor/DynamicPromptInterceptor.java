package com.fun.novel.ai.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.fun.novel.ai.listener.ToolCallListener;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.Map;

/**
 * 动态提示拦截器
 */
public class DynamicPromptInterceptor extends ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 首先尝试从监听器获取最新的城市名
        ToolCallListener toolCallListener = new ToolCallListener();
        String cityName = toolCallListener.getCurrentCity();

        
        // 只有在找到了城市名的情况下才打印
        SystemMessage enhancedSystemMessage = null;
        String  dynamicPrompt;
        if (cityName != null) {
            System.out.println("DynamicPromptInterceptor received cityName: " + cityName);
            switch (cityName) {
                case "武汉":
                    dynamicPrompt = "现在武汉的天气真好";
                    break;
                default:
                    dynamicPrompt = "现在城市天气真好";
                    break;
            }
            enhancedSystemMessage=new SystemMessage(request.getSystemMessage().getText() +
                        "\n" + dynamicPrompt);
//            System.out.println("DynamicPromptInterceptor modified system message: " + enhancedSystemMessage.getText());
        } else {
            enhancedSystemMessage=new SystemMessage(request.getSystemMessage().getText());
//            System.out.println("DynamicPromptInterceptor did not find cityName in context");
        }



        ModelRequest modified = ModelRequest.builder(request)
                .systemMessage(enhancedSystemMessage)
                .build();
        return handler.call(modified);
    }

    @Override
    public String getName() {
        return "DynamicPromptInterceptor";
    }
}
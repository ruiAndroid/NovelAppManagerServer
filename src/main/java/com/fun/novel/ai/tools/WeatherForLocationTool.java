package com.fun.novel.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import com.fun.novel.ai.listener.ToolCallListener;

public class WeatherForLocationTool implements ToolCallback {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 注意：在实际项目中，你可能需要通过其他方式注入这个监听器
    private static ToolCallListener toolCallListener = new ToolCallListener();

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("getWeatherForLocation")
                .description("获取指定城市的天气")
                .inputSchema(ToolSchemas.WEATHER_TOOL_INPUT_SCHEMA)
                .build();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        System.out.println("WeatherForLocationTool received toolInput: " + toolInput);
        
        // 通知监听器工具被调用
        toolCallListener.onWeatherToolCalled(toolInput);
        
        try {
            // 解析JSON参数
            JsonNode jsonNode = objectMapper.readTree(toolInput);
            String cityName = jsonNode.get("city_name").asText();
            return callWeatherService(cityName);
        } catch (Exception e) {
            System.err.println("Error parsing weather tool input: " + e.getMessage());
            // 如果解析失败，尝试直接使用输入作为城市名
            return callWeatherService(toolInput);
        }
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }

    private String callWeatherService(String city) {
        return "大太阳 " + city + "!";
    }
}
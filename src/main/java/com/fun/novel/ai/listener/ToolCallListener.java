package com.fun.novel.ai.listener;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToolCallListener {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储线程本地的city_name
    private static final ThreadLocal<String> currentCity = new ThreadLocal<>();
    
    /**
     * 当WeatherForLocationTool被调用时，更新city_name到上下文
     */
    public void onWeatherToolCalled(String toolInput) {
        try {
            // 解析工具输入参数
            JsonNode jsonNode = objectMapper.readTree(toolInput);
            String cityName = jsonNode.get("city_name").asText();
            
            // 设置当前线程的城市名
            currentCity.set(cityName);
            System.out.println("ToolCallListener: Updated city_name to " + cityName);
        } catch (Exception e) {
            System.err.println("ToolCallListener: Error parsing tool input: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前线程的城市名
     */
    public String getCurrentCity() {
        return currentCity.get();
    }
    
    /**
     * 清除当前线程的城市名
     */
    public void clearCurrentCity() {
        currentCity.remove();
    }
}
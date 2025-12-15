package com.fun.novel.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class UserLocationTool implements ToolCallback {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("getUserLocation")
                .description("根据user_id获取所在城市")
                .inputSchema(ToolSchemas.USER_LOCATION_TOOL_INPUT_SCHEMA)
                .build();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        System.out.println("UserLocationTool received toolInput: " + toolInput);
        try {
            // 解析JSON参数
            JsonNode jsonNode = objectMapper.readTree(toolInput);
            String userQuery = jsonNode.get("user_query").asText();
            System.out.println("UserLocationTool parsed userQuery: " + userQuery);
            return callLocationService(userQuery);
        } catch (Exception e) {
            System.err.println("Error parsing location tool input: " + e.getMessage());
            // 如果解析失败，尝试直接使用输入作为查询参数
            return callLocationService(toolInput);
        }
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        System.out.println("UserLocationTool received toolContext: " + (toolContext != null ? toolContext.getContext() : "null"));
        return call(toolInput);
    }

    private String callLocationService(String query) {
        System.out.println("UserLocationTool processing query: " + query);
        
        // 根据查询参数确定位置
        String location = "武汉";
        System.out.println("UserLocationTool returning location: " + location);
        return location;
    }
}
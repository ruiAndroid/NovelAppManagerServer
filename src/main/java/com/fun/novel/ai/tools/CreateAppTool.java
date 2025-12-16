package com.fun.novel.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateAppTool implements ToolCallback {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(CreateAppTool.class);
    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("createNovelApp")
                .description("创建一个新的小说类小程序")
                .inputSchema("""
                    {
                      "type": "object",
                      "properties": {
                        "platform": {
                          "type": "string",
                          "description": "目标平台，可选值：douyin(抖音)、kuaishou(快手)、weixin(微信)、baidu(百度)",
                          "enum": ["douyin", "kuaishou", "weixin", "baidu"]
                        },
                        "appName": {
                          "type": "string",
                          "description": "小程序的名称"
                        }
                      },
                      "required": ["platform", "appName"]
                    }
                    """)
                .build();
    }
    
    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }
    
    @Override
    public String call(String toolInput) {
        try {
            JsonNode jsonNode = objectMapper.readTree(toolInput);
            String platform = jsonNode.get("platform").asText();
            String appName = jsonNode.get("appName").asText();
            logger.info(String.format("根据用户需求生成的小程序信息: %s", toolInput));

            // 这里应该调用实际的创建小程序服务
            // 暂时返回模拟结果
            return String.format("已成功创建名为'%s'的%s小程序", appName, getPlatformName(platform));
        } catch (Exception e) {
            return "创建小程序时出现错误: " + e.getMessage();
        }
    }
    
    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }
    
    private String getPlatformName(String platformCode) {
        switch (platformCode) {
            case "douyin": return "抖音";
            case "kuaishou": return "快手";
            case "weixin": return "微信";
            case "baidu": return "百度";
            default: return platformCode;
        }
    }
}
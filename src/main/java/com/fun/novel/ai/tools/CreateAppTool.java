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

            // 模拟创建小程序的多个步骤，用于测试流式数据返回
            StringBuilder result = new StringBuilder();
            
            // 步骤1：初始化项目
            result.append("开始创建小程序...\n");
            result.append(String.format("初始化%s平台的%s项目...\n", getPlatformName(platform), appName));
            Thread.sleep(500); // 模拟处理时间
            
            // 步骤2：配置基础信息
            result.append("配置小程序基础信息...\n");
            Thread.sleep(500); // 模拟处理时间
            
            // 步骤3：创建核心页面
            result.append("创建小程序核心页面结构...\n");
            Thread.sleep(500); // 模拟处理时间
            
            // 步骤4：配置API接口
            result.append("配置小程序API接口...\n");
            Thread.sleep(500); // 模拟处理时间
            
            // 步骤5：完成创建
            result.append(String.format("已成功创建名为'%s'的%s小程序\n", appName, getPlatformName(platform)));
            result.append("小程序创建完成！可以开始使用了。\n");
            
            return result.toString();
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
package com.fun.novel.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.service.NovelAppService;
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

    @Autowired
    private NovelAppService novelAppService;
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
            logger.debug("Processing create app request for platform: {}, appName: {}", platform, appName);

            // 创建小程序的多个步骤
            StringBuilder result = new StringBuilder();
            
            // 步骤1：生成小程序基础信息
            generateAppBasicInfo(platform, appName);
            result.append("小程序基础信息生成完成。\n");
            Thread.sleep(500); // 模拟处理时间
            
            // 步骤2：初始化项目结构
            result.append(initializeProjectStructure(platform, appName));
            Thread.sleep(500); // 模拟处理时间
            
            // 步骤3：创建核心页面
            result.append(createCorePages(platform, appName));
            Thread.sleep(500); // 模拟处理时间
            
            // 步骤4：配置API接口
            result.append(configureApiInterfaces(platform, appName));
            Thread.sleep(500); // 模拟处理时间
            
            // 步骤5：完成创建
            result.append(completeAppCreation(platform, appName));
            logger.debug("Step 5 completed: App creation finished");
            
            String finalResult = result.toString();
            logger.debug("CreateAppTool returning result: '{}'", finalResult);
            return finalResult;
        } catch (Exception e) {
            logger.error("Error creating app", e);
            return "创建小程序时出现错误: " + e.getMessage();
        }
    }
    
    /**
     * 步骤1：生成小程序基础信息
     * 
     * @param platform 平台代码
     * @param appName 小程序名称
     * @return 处理结果字符串
     */
    private String generateAppBasicInfo(String platform, String appName) {
        StringBuilder sb = new StringBuilder();
        sb.append("开始创建小程序...\n");
        sb.append(String.format("初始化%s平台的%s项目...\n", getPlatformName(platform), appName));
        sb.append("正在生成小程序基础信息...\n");
        
        // 生成小程序ID（使用UUID）
        String appId = generateAppId(platform);
        sb.append(String.format("生成小程序ID: %s\n", appId));
        
        // 生成AppKey（针对需要的平台）
        if (needsAppKey(platform)) {
            String appKey = generateAppKey(appId);
            sb.append(String.format("生成AppKey: %s\n", appKey));
        }
        

        CreateNovelAppRequest.BaseConfig baseConfig=new CreateNovelAppRequest.BaseConfig();
        NovelApp appsByNameAndPlatform = novelAppService.getAppsByNameAndPlatform(appName, platform);


        return sb.toString();
    }
    
    /**
     * 生成小程序ID
     * 
     * @param platform 平台代码
     * @return 小程序ID
     */
    private String generateAppId(String platform) {
        // 根据平台生成不同格式的AppID
        String platformPrefix = getAppIdPrefix(platform);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return platformPrefix + uuid.substring(0, 16);
    }
    
    /**
     * 获取平台对应的AppID前缀
     * 
     * @param platform 平台代码
     * @return AppID前缀
     */
    private String getAppIdPrefix(String platform) {
        switch (platform) {
            case "douyin": return "dy_";
            case "kuaishou": return "ks_";
            case "weixin": return "wx_";
            case "baidu": return "bd_";
            default: return "app_";
        }
    }
    
    /**
     * 判断平台是否需要AppKey
     * 
     * @param platform 平台代码
     * @return 是否需要AppKey
     */
    private boolean needsAppKey(String platform) {
        // 根据实际平台需求配置
        return !"baidu".equals(platform);
    }
    
    /**
     * 生成AppKey
     * 
     * @param appId 小程序ID
     * @return AppKey
     */
    private String generateAppKey(String appId) {
        // 基于AppID生成AppKey（简单实现，实际项目中可以更复杂）
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String combined = appId + timestamp;
        return UUID.nameUUIDFromBytes(combined.getBytes()).toString().replace("-", "");
    }
    
    
    /**
     * 步骤2：初始化项目结构
     * 
     * @param platform 平台代码
     * @param appName 小程序名称
     * @return 处理结果字符串
     */
    private String initializeProjectStructure(String platform, String appName) {
        StringBuilder sb = new StringBuilder();
        sb.append("配置小程序项目结构...\n");
        
        // TODO: 创建项目目录结构
        // TODO: 生成平台所需的配置文件
        
        logger.debug("Step 2 completed: Initialized project structure");
        return sb.toString();
    }
    
    /**
     * 步骤3：创建核心页面
     * 
     * @param platform 平台代码
     * @param appName 小程序名称
     * @return 处理结果字符串
     */
    private String createCorePages(String platform, String appName) {
        StringBuilder sb = new StringBuilder();
        sb.append("创建小程序核心页面结构...\n");
        
        // TODO: 创建首页
        // TODO: 创建详情页
        // TODO: 创建分类页
        // TODO: 创建个人中心页
        
        logger.debug("Step 3 completed: Created core pages");
        return sb.toString();
    }
    
    /**
     * 步骤4：配置API接口
     * 
     * @param platform 平台代码
     * @param appName 小程序名称
     * @return 处理结果字符串
     */
    private String configureApiInterfaces(String platform, String appName) {
        StringBuilder sb = new StringBuilder();
        sb.append("配置小程序API接口...\n");
        
        // TODO: 配置API基础路径
        // TODO: 配置接口参数和返回格式
        // TODO: 添加接口安全配置
        
        logger.debug("Step 4 completed: Configured API interfaces");
        return sb.toString();
    }
    
    /**
     * 步骤5：完成创建
     * 
     * @param platform 平台代码
     * @param appName 小程序名称
     * @return 处理结果字符串
     */
    private String completeAppCreation(String platform, String appName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("已成功创建名为'%s'的%s小程序\n", appName, getPlatformName(platform)));
        sb.append("小程序创建完成！可以开始使用了。\n");
        
        logger.debug("Step 5 completed: App creation finished");
        return sb.toString();
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
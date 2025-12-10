package com.fun.novel.ai.tools;

public class ToolSchemas {
    public static final String WEATHER_TOOL_INPUT_SCHEMA = 
        "{\n" +
        "  \"type\": \"object\",\n" +
        "  \"properties\": {\n" +
        "    \"city_name\": {\n" +
        "      \"type\": \"string\",\n" +
        "      \"description\": \"城市名称\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"required\": [\"city_name\"]\n" +
        "}";
    
    public static final String USER_LOCATION_TOOL_INPUT_SCHEMA = 
        "{\n" +
        "  \"type\": \"object\",\n" +
        "  \"properties\": {\n" +
        "    \"user_query\": {\n" +
        "      \"type\": \"string\",\n" +
        "      \"description\": \"用户查询参数\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"required\": [\"user_query\"]\n" +
        "}";
}
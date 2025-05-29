package com.fun.novel.utils.publish;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class BaiduPublishCommand implements PlatformPublishCommand {
    @Override
    public String buildCommand(String appId, String projectPath, Map<String, String> extraParams) {
        return "npx @vue/cli-service uni-publish --platform mp-baidu";
    }

    @Override
    public String getPlatformCode() {
        return "mp-baidu";
    }

    @Override
    public String getPlatformName() {
        return "百度小程序";
    }
} 
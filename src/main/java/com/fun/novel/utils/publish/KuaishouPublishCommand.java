package com.fun.novel.utils.publish;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class KuaishouPublishCommand implements PlatformPublishCommand {
    @Override
    public String buildCommand(String appId, String projectPath, Map<String, String> extraParams) {
        return "npx @vue/cli-service uni-publish --platform mp-kuaishou";
    }

    @Override
    public String getPlatformCode() {
        return "mp-kuaishou";
    }

    @Override
    public String getPlatformName() {
        return "快手小程序";
    }
} 
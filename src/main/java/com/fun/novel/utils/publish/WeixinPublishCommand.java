package com.fun.novel.utils.publish;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WeixinPublishCommand implements PlatformPublishCommand {
    @Override
    public String buildCommand(String appId, String projectPath, Map<String, String> extraParams) {
        return "npx @vue/cli-service uni-publish --platform mp-weixin";
    }

    @Override
    public String getPlatformCode() {
        return "mp-weixin";
    }

    @Override
    public String getPlatformName() {
        return "微信小程序";
    }
} 
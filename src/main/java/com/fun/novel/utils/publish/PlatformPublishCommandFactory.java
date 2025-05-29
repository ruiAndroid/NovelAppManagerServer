package com.fun.novel.utils.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlatformPublishCommandFactory {
    private final Map<String, PlatformPublishCommand> commandMap = new HashMap<>();

    @Autowired
    private List<PlatformPublishCommand> commands;

    @PostConstruct
    public void init() {
        for (PlatformPublishCommand command : commands) {
            commandMap.put(command.getPlatformCode(), command);
        }
    }

    public PlatformPublishCommand getCommand(String platformCode) {
        PlatformPublishCommand command = commandMap.get(platformCode);
        if (command == null) {
            throw new IllegalArgumentException("不支持的平台代码: " + platformCode);
        }
        return command;
    }

    public Map<String, String> getPlatformNames() {
        Map<String, String> platformNames = new HashMap<>();
        for (PlatformPublishCommand command : commands) {
            platformNames.put(command.getPlatformCode(), command.getPlatformName());
        }
        return platformNames;
    }
} 
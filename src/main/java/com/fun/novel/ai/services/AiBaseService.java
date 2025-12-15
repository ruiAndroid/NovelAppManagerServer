package com.fun.novel.ai.services;

import com.fun.novel.ai.utils.ModelsUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class AiBaseService {

    public Set<Map<String, String>> getDashScope() {

        Set<Map<String, String>> resultSet;

        try {
            resultSet = ModelsUtils.getDashScopeModels();
        }
        catch (IOException e) {
            throw new RuntimeException("Get DashScope Model failed, " + e.getMessage());
        }

        return resultSet;
    }
}

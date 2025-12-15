package com.fun.novel.ai.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fun.novel.ai.entity.DashScopeModel;
import com.fun.novel.ai.entity.DashScopeModels;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 加载和处理模型信息工具类
 */
public class ModelsUtils {

    //从 models.yaml 配置文件中读取模型数据
    private final static String MODELS_FILE_PATH = "models.yaml";
    //模型名称
    private static final String MODEL = "model";
    //模型描述
    private static final String DESC = "desc";

    private ModelsUtils() {

    }

    /**
     * 实体类映射YAML数据结构
     * 主要用于为应用程序提供可用的模型列表及其描述信息
     * @return
     * @throws IOException
     */
    public static Set<Map<String, String>> getDashScopeModels() throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        InputStream resourceAsStream = ModelsUtils.class.getClassLoader().getResourceAsStream(MODELS_FILE_PATH);
        DashScopeModels models = mapper.readValue(resourceAsStream, DashScopeModels.class);

        Set<Map<String, String>> resultSet = new HashSet<>();
        for (DashScopeModel model : models.getDashScope()) {
            Map<String, String> modelMap = new HashMap<>();
            modelMap.put(MODEL, model.getName());
            modelMap.put(DESC, model.getDescription());
            resultSet.add(modelMap);
        }

        return resultSet;
    }

}

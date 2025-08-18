package com.fun.novel.service.fileOpeartionService;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.utils.CreateNovelTaskLogger;
import com.fun.novel.dto.CreateNovelLogType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.lang.Runnable;

/**
 * 配置文件操作服务的抽象基类
 * 提供通用的文件操作方法和工具函数
 */
public abstract class AbstractConfigFileOperationService {
    
    @Autowired
    protected CreateNovelTaskLogger taskLogger;
    
    @Value("${build.workPath}")
    protected String buildWorkPath;
    
    protected static final int FILE_STEP_DELAY_MS = 1000;
    
    /**
     * 删除目录及其所有内容
     * @param path 目录路径
     * @throws IOException IO异常
     */
    protected void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.notExists(path)) return;
        Files.walk(path)
                .sorted((p1, p2) -> p2.toString().compareTo(p1.toString())) // 逆序排列
                .forEach(p -> {
                    try { 
                        Files.deleteIfExists(p); 
                    } catch (IOException ignore) {}
                });
    }
    
    /**
     * 平台类型转换为键名
     * @param platform 平台类型
     * @return 键名
     */
    protected String platformToKey(String platform) {
        switch (platform) {
            case "douyin": return "tt";
            case "kuaishou": return "ks";
            case "weixin": return "wx";
            case "baidu": return "bd";
            default: return platform;
        }
    }
    
    /**
     * 构建支付类型配置映射
     * @param payTypeConfig 支付类型配置
     * @return 映射对象
     */
    protected java.util.Map<String, Object> buildPayTypeMap(CreateNovelAppRequest.PayTypeConfig payTypeConfig) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (payTypeConfig == null || !Boolean.TRUE.equals(payTypeConfig.getEnabled())) {
            map.put("enable", false);
        } else {
            map.put("enable", true);
            java.util.Map<String, Object> gatewayId = new java.util.LinkedHashMap<>();
            try {
                gatewayId.put("android", payTypeConfig.getGatewayAndroid() == null ? 0 : Integer.parseInt(payTypeConfig.getGatewayAndroid()));
            } catch (Exception e) { gatewayId.put("android", payTypeConfig.getGatewayAndroid()); }
            try {
                gatewayId.put("ios", payTypeConfig.getGatewayIos() == null ? 0 : Integer.parseInt(payTypeConfig.getGatewayIos()));
            } catch (Exception e) { gatewayId.put("ios", payTypeConfig.getGatewayIos()); }
            map.put("gateway_id", gatewayId);
        }
        return map;
    }
    
    /**
     * 在switch语句中插入case语句（兼容处理）
     * @param lines 文件行列表
     * @param funcName 函数名
     * @param buildCode 构建代码
     * @param varName 变量名
     */
    protected void insertSwitchCaseCompat(List<String> lines, String funcName, String buildCode, String varName) {
        String caseStr = "        case '" + buildCode + "':\n            return " + varName + ";";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("function " + funcName) || line.matches(".*(const|let)\\s+" + funcName + "\\s*=.*=>.*\\{")) {
                // 找到switch
                int switchIdx = -1;
                for (int j = i; j < lines.size(); j++) {
                    if (lines.get(j).contains("switch")) {
                        switchIdx = j;
                        break;
                    }
                }
                if (switchIdx != -1) {
                    // 检查是否已存在该case
                    boolean already = false;
                    int insertIdx = switchIdx + 1;
                    while (insertIdx < lines.size() && !lines.get(insertIdx).contains("}")) {
                        if (lines.get(insertIdx).contains("case '" + buildCode + "':")) {
                            already = true;
                            break;
                        }
                        insertIdx++;
                    }
                    if (!already) {
                        lines.add(insertIdx, caseStr);
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 备份文件并添加回滚操作
     * @param taskId 任务ID
     * @param filePath 文件路径
     * @param rollbackActions 回滚操作列表
     * @throws IOException IO异常
     */
    protected void backupFileAndAddRollback(String taskId, Path filePath, List<Runnable> rollbackActions) throws IOException {
        if (Files.exists(filePath)) {
            String backupPath = filePath.toString() + ".bak";
            Path backup = java.nio.file.Paths.get(backupPath);
            Files.copy(filePath, backup, StandardCopyOption.REPLACE_EXISTING);
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：还原" + filePath.getFileName(), CreateNovelLogType.ERROR);
                    Files.copy(backup, filePath, StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(backup);
                } catch (Exception ignore) {}
            });
        } else {
            // 文件不存在，回滚时删除
            rollbackActions.add(() -> {
                try {
                    taskLogger.log(taskId, "回滚动作：删除" + filePath.getFileName(), CreateNovelLogType.ERROR);
                    Files.deleteIfExists(filePath);
                } catch (Exception ignore) {}
            });
        }
    }
    
    /**
     * 删除备份文件
     * @param backupPath 备份文件路径
     */
    protected void deleteBackupFile(String backupPath) {
        try { 
            Files.deleteIfExists(java.nio.file.Paths.get(backupPath)); 
        } catch (Exception ignore) {}
    }
}
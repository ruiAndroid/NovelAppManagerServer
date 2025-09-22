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
        if (payTypeConfig == null) {
            map.put("enable", false);
        } else {
            map.put("enable", Boolean.TRUE.equals(payTypeConfig.getEnabled()));
            
            // 即使enable为false，也处理gateway_id参数
            java.util.Map<String, Object> gatewayId = new java.util.LinkedHashMap<>();
            try {
                gatewayId.put("android", payTypeConfig.getGatewayAndroid() == null ? 0 : Integer.parseInt(payTypeConfig.getGatewayAndroid()));
            } catch (Exception e) { 
                gatewayId.put("android", payTypeConfig.getGatewayAndroid()); 
            }
            try {
                gatewayId.put("ios", payTypeConfig.getGatewayIos() == null ? 0 : Integer.parseInt(payTypeConfig.getGatewayIos()));
            } catch (Exception e) { 
                gatewayId.put("ios", payTypeConfig.getGatewayIos()); 
            }
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
            // 改进函数名匹配逻辑，确保能正确匹配各种形式的函数声明
            if (line.contains("function " + funcName) || 
                line.matches(".*(const|let)\\s+" + funcName + "\\s*=.*=>.*") || 
                line.matches(".*" + funcName + "\\s*=.*=>.*") ||
                line.matches(".*function\\s+" + funcName + ".*")) {
                
                // 找到函数开始位置后，寻找switch语句
                int switchIdx = -1;
                int funcStartIdx = i;
                
                // 找到函数体开始的 '{'
                for (int j = i; j < lines.size(); j++) {
                    if (lines.get(j).contains("{")) {
                        funcStartIdx = j;
                        break;
                    }
                }
                
                // 在函数体内查找switch语句
                int braceCount = 0;
                boolean inFuncBody = false;
                for (int j = funcStartIdx; j < lines.size(); j++) {
                    String currentLine = lines.get(j).trim();
                    
                    if (currentLine.contains("{")) {
                        if (!inFuncBody) {
                            inFuncBody = true;
                        } else {
                            braceCount++;
                        }
                    }
                    
                    if (currentLine.contains("}")) {
                        if (braceCount > 0) {
                            braceCount--;
                        } else if (inFuncBody) {
                            // 函数体结束
                            break;
                        }
                    }
                    
                    // 在函数体内查找switch语句
                    if (inFuncBody && currentLine.contains("switch") && currentLine.contains("(")) {
                        switchIdx = j;
                        break;
                    }
                }
                
                if (switchIdx != -1) {
                    // 查找switch语句块的结束位置
                    int switchEndIdx = -1;
                    braceCount = 0;
                    boolean inSwitchBlock = false;
                    
                    for (int k = switchIdx; k < lines.size(); k++) {
                        String currentLine = lines.get(k);
                        if (currentLine.contains("{") && !inSwitchBlock) {
                            inSwitchBlock = true;
                            continue;
                        }
                        
                        if (inSwitchBlock) {
                            if (currentLine.contains("{")) {
                                braceCount++;
                            }
                            if (currentLine.contains("}")) {
                                if (braceCount > 0) {
                                    braceCount--;
                                } else {
                                    switchEndIdx = k;
                                    break;
                                }
                            }
                        }
                    }
                    
                    // 检查是否已存在该case
                    boolean already = false;
                    int insertIdx = switchIdx;
                    
                    if (switchEndIdx > switchIdx) {
                        // 在switch语句块内查找是否已存在该case
                        for (int k = switchIdx + 1; k < switchEndIdx; k++) {
                            if (lines.get(k).contains("case '" + buildCode + "':")) {
                                already = true;
                                break;
                            }
                        }
                        
                        // 如果未找到已存在的case，则在switch结束前插入
                        if (!already) {
                            // 找到合适的位置插入（在switch块结束前）
                            insertIdx = switchEndIdx;
                            
                            // 向前查找合适的位置
                            while (insertIdx > switchIdx + 1) {
                                String prevLine = lines.get(insertIdx - 1).trim();
                                if (!prevLine.equals("") && !prevLine.equals("}")) {
                                    break;
                                }
                                insertIdx--;
                            }
                            
                            lines.add(insertIdx, caseStr);
                        }
                    }
                }
                // 处理完一个函数后跳出循环，避免重复处理
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

    /**
     * 格式化JSON字符串，外层键使用单引号，内层键不使用引号并修复缩进
     * @param jsonString JSON字符串
     * @return 格式化后的字符串
     */
    protected static String formatJsonString(String jsonString) {
        // 将带引号的键名替换为不带引号的键名（用于内层键）
        jsonString = jsonString.replaceAll("\"([a-zA-Z_][a-zA-Z0-9_]*)\"\\s*:", "$1:");

        // 为外层键添加单引号 - 处理所有平台键
        jsonString = jsonString.replaceAll("\"(tt|ks|wx|bd)\"\\s*:", "'$1':");
        jsonString = jsonString.replaceAll("(\\{|,\\s+)\\b(tt|ks|wx|bd)\\b\\s*:", "$1'$2':");
        // 特殊处理第一个键（对象开始后的第一个键）
        jsonString = jsonString.replaceAll("\\{\\s*(tt|ks|wx|bd)\\s*:", "{'$1':");

        // 创建格式化的结果
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        String[] lines = jsonString.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 判断是否是结束括号
            if (line.startsWith("}") || line.startsWith("]")) {
                indentLevel--;
            }

            // 添加缩进
            for (int i = 0; i < indentLevel; i++) {
                result.append("    ");
            }

            result.append(line).append("\n");

            // 判断是否是开始括号
            if (line.endsWith("{") || line.endsWith("[")) {
                indentLevel++;
            }

            // 处理同时包含开始和结束括号的情况
            if (line.contains("{") && line.contains("}")) {
                // 同一行包含开始和结束，不改变缩进级别
            }
        }

        // 返回结果，去掉最后的换行符
        String formatted = result.toString();
        if (formatted.endsWith("\n")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }

        return formatted;
    }
}
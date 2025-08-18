package com.fun.novel.enums;

/**
 * 操作日志类型枚举
 */
public enum OpType {
    /**
     * 查询操作
     */
    QUERY(1, "查询"),
    
    /**
     * 新增操作
     */
    INSERT(2, "新增"),
    
    /**
     * 修改操作
     */
    UPDATE(3, "修改"),
    
    /**
     * 删除操作
     */
    DELETE(4, "删除"),
    
    /**
     * 导出操作
     */
    EXPORT(5, "导出"),
    
    /**
     * 导入操作
     */
    IMPORT(6, "导入"),
    
    /**
     * 其他操作
     */
    OTHER(0, "其他");

    public static final int QUERY_CODE = 1;
    public static final int INSERT_CODE = 2;
    public static final int UPDATE_CODE = 3;
    public static final int DELETE_CODE = 4;
    public static final int EXPORT_CODE = 5;
    public static final int IMPORT_CODE = 6;
    public static final int OTHER_CODE = 0;

    private final int code;
    private final String info;

    OpType(int code, String info) {
        this.code = code;
        this.info = info;
    }

    public int getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }
    
}
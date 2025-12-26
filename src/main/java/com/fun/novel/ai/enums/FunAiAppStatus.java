package com.fun.novel.ai.enums;

/**
 * FunAI 应用状态机（6态）
 *
 * 0 CREATED   : 空壳/草稿（仅创建记录，尚未上传代码）
 * 1 UPLOADED  : 已上传zip（可部署）
 * 2 DEPLOYING : 部署中（解压/安装依赖/构建中）
 * 3 READY     : 部署成功（dist 已生成，可访问）
 * 4 FAILED    : 部署失败（可查看 lastDeployError，允许重新上传/重试）
 * 5 DISABLED  : 禁用（可选）
 */
public enum FunAiAppStatus {
    CREATED(0, "空壳/草稿"),
    UPLOADED(1, "已上传"),
    DEPLOYING(2, "部署中"),
    READY(3, "可访问"),
    FAILED(4, "部署失败"),
    DISABLED(5, "禁用");

    private final int code;
    private final String desc;

    FunAiAppStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int code() {
        return code;
    }

    public String desc() {
        return desc;
    }
}



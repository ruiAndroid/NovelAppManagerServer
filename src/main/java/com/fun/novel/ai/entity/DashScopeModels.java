package com.fun.novel.ai.entity;


import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 大模型列表实体类
 */
public class DashScopeModels implements Serializable {
    @Serial
    private static final long serialVersionUID = 2123534567887673L;

    private List<DashScopeModel> dashScope;

    public List<DashScopeModel> getDashScope() {
        return dashScope;
    }

    public void setDashScope(List<DashScopeModel> dashScope) {
        this.dashScope = dashScope;
    }
}

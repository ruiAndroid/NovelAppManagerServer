package com.fun.novel.ai.entity;

import java.io.Serial;
import java.io.Serializable;

public class AppTheme implements Serializable {

    @Serial
    private static final long serialVersionUID = 2123534567887673L;
    private String name;
    private String mainTheme;
    private String secondTheme;
    private Integer payCardStyle;
    private Integer homeCardStyle;

    public AppTheme(String name, String mainTheme, String secondTheme, Integer payCardStyle, Integer homeCardStyle) {
        this.name=name;
        this.mainTheme=mainTheme;
        this.secondTheme=secondTheme;
        this.payCardStyle=payCardStyle;
        this.homeCardStyle=homeCardStyle;
    }

    public Integer getPayCardStyle() {
        return payCardStyle;
    }

    public void setPayCardStyle(Integer payCardStyle) {
        this.payCardStyle = payCardStyle;
    }

    public Integer getHomeCardStyle() {
        return homeCardStyle;
    }

    public void setHomeCardStyle(Integer homeCardStyle) {
        this.homeCardStyle = homeCardStyle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMainTheme() {
        return mainTheme;
    }

    public void setMainTheme(String mainTheme) {
        this.mainTheme = mainTheme;
    }

    public String getSecondTheme() {
        return secondTheme;
    }

    public void setSecondTheme(String secondTheme) {
        this.secondTheme = secondTheme;
    }
}

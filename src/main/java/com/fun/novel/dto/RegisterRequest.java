package com.fun.novel.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String userName;
    private String password;
    private String phone;
    private String avatar;
    private Integer type;
}
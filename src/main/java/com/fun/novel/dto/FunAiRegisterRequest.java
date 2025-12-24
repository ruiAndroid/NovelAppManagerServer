package com.fun.novel.dto;

import lombok.Data;

@Data
public class FunAiRegisterRequest {
    private String userName;
    private String password;
    private String phone;
    private String avatar;
    private String email;
}
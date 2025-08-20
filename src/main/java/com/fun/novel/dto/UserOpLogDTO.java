package com.fun.novel.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fun.novel.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserOpLogDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Integer opType;
    private Integer opStatus;
    private String methodName;
    private String requestType;
    private String requestUrl;
    private String requestIp;
    private String requestParams;
    private String responseResult;
    private LocalDateTime updateTime;


}
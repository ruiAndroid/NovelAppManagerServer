package com.fun.novel.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserOpArchiveDTO {
    private Long userId;
    private String userName;
    private Integer opType;
    private Integer opStatus;
    private String opName;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private String updateTime;


}
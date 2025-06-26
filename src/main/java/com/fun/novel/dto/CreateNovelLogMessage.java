package com.fun.novel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNovelLogMessage {

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime timestamp;

    private String message;

    private String type;

    public static CreateNovelLogMessage from(String message, CreateNovelLogType type) {
        return new CreateNovelLogMessage(LocalTime.now(), message, type.name().toLowerCase());
    }
} 
package com.shorthand.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminWordVo {
    private Long id;
    private String word;
    private String normalized;
    private String phoneticUk;
    private String phoneticUs;
    private String pos;
    private String meaning;
    private String exampleEn;
    private String exampleCn;
    private LocalDateTime updateTime;
    private String audioUrl;
}

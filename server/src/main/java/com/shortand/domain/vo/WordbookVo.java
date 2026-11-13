package com.shortand.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WordbookVo {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String cover;
    private Integer difficulty;
    private Integer wordCount;
    private Integer status;
    private Integer currentRoundNo;
    private Integer learnedCount;
    private LocalDateTime lastStudyTime;
}

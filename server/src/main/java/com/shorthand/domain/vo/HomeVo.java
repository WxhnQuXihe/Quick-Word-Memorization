package com.shorthand.domain.vo;

import lombok.Data;

@Data
public class HomeVo {
    private WordbookVo currentBook;
    private long favoriteCount;
    private long knownCount;
    private long learnedCount;
    private Integer streakDays;
}

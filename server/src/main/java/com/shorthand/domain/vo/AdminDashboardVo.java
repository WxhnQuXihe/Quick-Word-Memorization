package com.shorthand.domain.vo;

import lombok.Data;

import com.shorthand.domain.ImportLog;

import java.util.List;

@Data
public class AdminDashboardVo {
    private long bookCount;
    private long wordCount;
    private long userCount;
    private long favoriteCount;
    private long knownCount;
    private long openFeedbackCount;
    private List<WordbookVo> books;
    private List<UserVo> recentUsers;
    private List<ImportLog> recentImports;
    private List<Long> activity;
    private long jvmUsedMb;
    private long jvmMaxMb;
    private long uptimeSeconds;
}

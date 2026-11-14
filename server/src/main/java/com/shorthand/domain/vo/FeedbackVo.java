package com.shorthand.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackVo {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String content;
    private String contact;
    private Integer status;
    private String reply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
}

package com.shorthand.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVo {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}

package com.shorthand.domain.vo;

import lombok.Data;

@Data
public class LoginVo {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
}

package com.shorthand.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserBo {
    @NotBlank(message = "请输入用户名")
    @Size(max = 32, message = "用户名最长 32 位")
    private String username;

    @NotBlank(message = "请输入初始密码")
    @Size(min = 6, max = 32, message = "密码长度 6~32 位")
    private String password;

    private String nickname;
}

package com.shorthand.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyBo {
    @NotBlank(message = "请输入回复内容")
    private String reply;
}

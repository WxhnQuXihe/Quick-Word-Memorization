package com.shorthand.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementBo {
    @NotBlank(message = "请输入公告标题")
    private String title;
    @NotBlank(message = "请输入公告内容")
    private String content;
    private Integer status;
}

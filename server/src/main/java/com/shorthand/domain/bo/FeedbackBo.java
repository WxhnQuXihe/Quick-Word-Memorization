package com.shorthand.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackBo {
    @NotBlank(message = "请写点反馈内容")
    @Size(max = 1000, message = "反馈内容最多 1000 字")
    private String content;

    @Size(max = 100, message = "联系方式最多 100 字")
    private String contact;
}

package com.shortand.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WordbookSaveBo {
    @NotBlank(message = "请输入词书编码")
    private String code;
    @NotBlank(message = "请输入词书名称")
    private String name;
    private String description;
    private String cover;
    private Integer difficulty;
    private Integer sort;
    private Integer status;
}

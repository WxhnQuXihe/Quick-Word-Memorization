package com.shortand.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WordSaveBo {
    @NotBlank(message = "请输入单词")
    private String word;
    private String phoneticUk;
    private String phoneticUs;
    private String pos;
    @NotBlank(message = "请输入释义")
    private String meaning;
    private String exampleEn;
    private String exampleCn;
}

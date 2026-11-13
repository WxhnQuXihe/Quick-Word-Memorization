package com.shorthand.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupVo {
    private Long groupId;
    private Integer roundNo;
    private Integer groupNo;
    private Integer wordCount;
    private Boolean finished;
    private List<WordVo> words;
}

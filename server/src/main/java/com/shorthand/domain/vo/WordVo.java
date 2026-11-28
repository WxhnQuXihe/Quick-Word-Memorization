package com.shorthand.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class WordVo {
    private Long id;
    private String word;
    private String phoneticUk;
    private String phoneticUs;
    private List<SenseVo> senses;
    private List<SenseVo> allSenses;
    private String audioUrl;
}

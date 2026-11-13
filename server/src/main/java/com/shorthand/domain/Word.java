package com.shorthand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("word")
public class Word {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String word;
    private String normalized;
    private String phoneticUk;
    private String phoneticUs;
    private String pos;
    private String meaning;
    private String exampleEn;
    private String exampleCn;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

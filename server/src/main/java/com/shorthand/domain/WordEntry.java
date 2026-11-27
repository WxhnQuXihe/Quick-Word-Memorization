package com.shorthand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("word_entry")
public class WordEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wordId;
    private String entryHash;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

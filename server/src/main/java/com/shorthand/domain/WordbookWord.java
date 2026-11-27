package com.shorthand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wordbook_word")
public class WordbookWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wordbookId;
    private Long wordId;
    private Long entryId;
    private Integer seq;
    private String unit;
}

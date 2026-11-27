package com.shorthand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("word_entry_sense")
public class WordEntrySense {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long entryId;
    private Long senseId;
    private Integer sort;
    private String displayMeaning;
}

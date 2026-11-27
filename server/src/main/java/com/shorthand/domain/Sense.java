package com.shorthand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sense")
public class Sense {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wordId;
    private String pos;
    private String posKey;
    private String meaning;
    private String meaningKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

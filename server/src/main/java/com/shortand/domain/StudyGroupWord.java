package com.shortand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_group_word")
public class StudyGroupWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long wordId;
    private Integer position;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

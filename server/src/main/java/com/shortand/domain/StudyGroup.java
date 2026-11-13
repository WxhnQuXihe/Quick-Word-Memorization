package com.shortand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_group")
public class StudyGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordbookId;
    private Integer roundNo;
    private Integer groupNo;
    private Integer wordCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}

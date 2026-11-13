package com.shorthand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_word")
public class UserWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    private Integer isFavorite;
    private LocalDateTime favoriteTime;
    private Integer isKnown;
    private LocalDateTime knownTime;
    private Integer seenCount;
    private LocalDateTime lastSeenTime;
    private Long firstWordbookId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

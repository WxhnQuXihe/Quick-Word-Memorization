package com.shorthand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("word_audio")
public class WordAudio {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wordId;
    private String accent;
    private String voice;
    private String objectKey;
    private String checksum;
    private Integer durationMs;
    private Integer sizeBytes;
    private String model;
    private String modelVersion;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

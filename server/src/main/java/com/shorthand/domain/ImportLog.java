package com.shorthand.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("import_log")
public class ImportLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wordbookId;
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer failRows;
    private Integer status;
    private String errorFile;
    private Long adminId;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}

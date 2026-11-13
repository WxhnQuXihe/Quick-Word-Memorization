package com.shorthand.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class WordImportVo {
    @ExcelIgnore
    private Integer rowNum;
    @ExcelProperty("序号")
    private Integer seq;
    @ExcelProperty("单词")
    private String word;
    @ExcelProperty("英式音标")
    private String phoneticUk;
    @ExcelProperty("美式音标")
    private String phoneticUs;
    @ExcelProperty("词性")
    private String pos;
    @ExcelProperty("释义")
    private String meaning;
    @ExcelProperty("英文例句")
    private String exampleEn;
    @ExcelProperty("例句翻译")
    private String exampleCn;
}

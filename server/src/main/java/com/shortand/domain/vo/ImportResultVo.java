package com.shortand.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultVo {
    private int totalRows;
    private int successRows;
    private int failRows;
    private List<String> errors = new ArrayList<>();
}

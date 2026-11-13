package com.shortand.service;

import com.shortand.domain.bo.WordbookSaveBo;
import com.shortand.domain.vo.WordbookVo;

import java.util.List;

public interface IAdminWordbookService {
    List<WordbookVo> list();

    WordbookVo create(WordbookSaveBo bo);

    WordbookVo update(Long id, WordbookSaveBo bo);

    void updateStatus(Long id, Integer status);
}

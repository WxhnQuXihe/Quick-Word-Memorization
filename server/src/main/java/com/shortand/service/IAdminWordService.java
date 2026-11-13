package com.shortand.service;

import com.shortand.common.PageResult;
import com.shortand.domain.bo.WordSaveBo;

public interface IAdminWordService {
    PageResult list(long pageNo, long pageSize, String keyword);

    Long create(WordSaveBo bo);

    void update(Long id, WordSaveBo bo);

    void delete(Long id);
}

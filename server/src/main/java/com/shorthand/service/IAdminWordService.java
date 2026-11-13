package com.shorthand.service;

import com.shorthand.common.PageResult;
import com.shorthand.domain.bo.WordSaveBo;

public interface IAdminWordService {
    PageResult list(long pageNo, long pageSize, String keyword);

    Long create(WordSaveBo bo);

    void update(Long id, WordSaveBo bo);

    void delete(Long id);
}

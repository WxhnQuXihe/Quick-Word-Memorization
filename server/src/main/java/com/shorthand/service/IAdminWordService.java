package com.shorthand.service;

import com.shorthand.common.PageResult;
import com.shorthand.domain.bo.WordSaveBo;
import org.springframework.web.multipart.MultipartFile;

public interface IAdminWordService {
    PageResult list(long pageNo, long pageSize, String keyword);

    Long create(WordSaveBo bo);

    void update(Long id, WordSaveBo bo);

    void delete(Long id);

    String uploadAudio(Long id, MultipartFile file);
}

package com.shorthand.service;

import com.shorthand.common.PageResult;
import com.shorthand.domain.vo.ImportResultVo;
import org.springframework.web.multipart.MultipartFile;

public interface IAdminImportService {
    ImportResultVo importExcel(MultipartFile file, Long wordbookId, Long adminId);

    PageResult logs(long pageNo, long pageSize);
}

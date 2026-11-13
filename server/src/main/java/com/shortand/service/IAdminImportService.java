package com.shortand.service;

import com.shortand.common.PageResult;
import com.shortand.domain.vo.ImportResultVo;
import org.springframework.web.multipart.MultipartFile;

public interface IAdminImportService {
    ImportResultVo importExcel(MultipartFile file, Long wordbookId, Long adminId);

    PageResult logs(long pageNo, long pageSize);
}

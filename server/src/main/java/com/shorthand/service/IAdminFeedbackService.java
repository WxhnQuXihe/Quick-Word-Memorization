package com.shorthand.service;

import com.shorthand.common.PageResult;

public interface IAdminFeedbackService {
    PageResult list(long pageNo, long pageSize, Integer status);

    void reply(Long id, String reply);
}

package com.shortand.service;

import com.shortand.common.PageResult;

public interface IAdminFeedbackService {
    PageResult list(long pageNo, long pageSize, Integer status);

    void reply(Long id, String reply);
}

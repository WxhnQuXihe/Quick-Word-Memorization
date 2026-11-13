package com.shorthand.service;

import com.shorthand.common.PageResult;
import com.shorthand.domain.bo.FeedbackBo;

public interface IFeedbackService {
    void submit(Long userId, FeedbackBo bo);

    PageResult mine(Long userId, long pageNo, long pageSize);
}

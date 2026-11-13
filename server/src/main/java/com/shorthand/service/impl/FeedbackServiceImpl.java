package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorthand.common.PageResult;
import com.shorthand.domain.Feedback;
import com.shorthand.domain.bo.FeedbackBo;
import com.shorthand.mapper.FeedbackMapper;
import com.shorthand.service.IFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements IFeedbackService {
    private final FeedbackMapper feedbackMapper;

    @Override
    public void submit(Long userId, FeedbackBo bo) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setContent(bo.getContent().trim());
        feedback.setContact(bo.getContact());
        feedback.setStatus(0);
        feedbackMapper.insert(feedback);
    }

    @Override
    public PageResult mine(Long userId, long pageNo, long pageSize) {
        Page<Feedback> page = feedbackMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<Feedback>lambdaQuery()
                        .eq(Feedback::getUserId, userId)
                        .orderByDesc(Feedback::getId));
        return PageResult.of(page.getRecords(), page.getTotal(), pageNo, pageSize);
    }
}

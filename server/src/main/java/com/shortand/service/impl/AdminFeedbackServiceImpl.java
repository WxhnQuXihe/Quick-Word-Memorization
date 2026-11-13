package com.shortand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortand.common.BizException;
import com.shortand.common.PageResult;
import com.shortand.domain.Feedback;
import com.shortand.mapper.FeedbackMapper;
import com.shortand.service.IAdminFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminFeedbackServiceImpl implements IAdminFeedbackService {
    private final FeedbackMapper feedbackMapper;

    @Override
    public PageResult list(long pageNo, long pageSize, Integer status) {
        Page<Feedback> page = feedbackMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<Feedback>lambdaQuery()
                        .eq(status != null, Feedback::getStatus, status)
                        .orderByDesc(Feedback::getId));
        return PageResult.of(page.getRecords(), page.getTotal(), pageNo, pageSize);
    }

    @Override
    public void reply(Long id, String reply) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) throw new BizException("反馈不存在");
        feedback.setReply(reply);
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setStatus(1);
        feedbackMapper.updateById(feedback);
    }
}

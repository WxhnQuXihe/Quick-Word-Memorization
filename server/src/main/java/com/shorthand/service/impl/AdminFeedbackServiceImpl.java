package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorthand.common.BizException;
import com.shorthand.common.PageResult;
import com.shorthand.domain.AppUser;
import com.shorthand.domain.Feedback;
import com.shorthand.mapper.AppUserMapper;
import com.shorthand.mapper.FeedbackMapper;
import com.shorthand.domain.vo.FeedbackVo;
import com.shorthand.service.IAdminFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFeedbackServiceImpl implements IAdminFeedbackService {
    private final FeedbackMapper feedbackMapper;
    private final AppUserMapper appUserMapper;

    @Override
    public PageResult list(long pageNo, long pageSize, Integer status) {
        Page<Feedback> page = feedbackMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<Feedback>lambdaQuery()
                        .eq(status != null, Feedback::getStatus, status)
                        .orderByDesc(Feedback::getId));
        List<FeedbackVo> vos = toVos(page.getRecords());
        return PageResult.of(vos, page.getTotal(), pageNo, pageSize);
    }

    private List<FeedbackVo> toVos(List<Feedback> rows) {
        if (rows.isEmpty()) return List.of();
        List<Long> userIds = rows.stream().map(Feedback::getUserId).distinct().toList();
        Map<Long, AppUser> users = appUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return rows.stream().map(f -> {
            FeedbackVo vo = new FeedbackVo();
            vo.setId(f.getId());
            vo.setUserId(f.getUserId());
            AppUser u = users.get(f.getUserId());
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setNickname(u.getNickname());
            }
            vo.setContent(f.getContent());
            vo.setContact(f.getContact());
            vo.setStatus(f.getStatus());
            vo.setReply(f.getReply());
            vo.setReplyTime(f.getReplyTime());
            vo.setCreateTime(f.getCreateTime());
            return vo;
        }).toList();
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

package com.shorthand.controller;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.FeedbackBo;
import com.shorthand.service.IFeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户-反馈")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final IFeedbackService feedbackService;

    @PostMapping
    public Result submit(@RequestAttribute("userId") Long userId, @Valid @RequestBody FeedbackBo bo) {
        feedbackService.submit(userId, bo);
        return Result.ok();
    }

    @GetMapping("/mine")
    public Result mine(@RequestAttribute("userId") Long userId,
                       @RequestParam(defaultValue = "1") long pageNo,
                       @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(feedbackService.mine(userId, pageNo, pageSize));
    }
}

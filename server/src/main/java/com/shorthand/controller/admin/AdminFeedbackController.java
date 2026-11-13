package com.shorthand.controller.admin;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.ReplyBo;
import com.shorthand.service.IAdminFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {
    private final IAdminFeedbackService adminFeedbackService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") long pageNo,
                       @RequestParam(defaultValue = "20") long pageSize,
                       @RequestParam(required = false) Integer status) {
        return Result.ok(adminFeedbackService.list(pageNo, pageSize, status));
    }

    @PostMapping("/{id}/reply")
    public Result reply(@PathVariable Long id, @Valid @RequestBody ReplyBo bo) {
        adminFeedbackService.reply(id, bo.getReply());
        return Result.ok();
    }
}

package com.shorthand.controller;

import com.shorthand.common.Result;
import com.shorthand.service.IAnnouncementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户-公告")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private final IAnnouncementService announcementService;

    @GetMapping
    public Result list() {
        return Result.ok(announcementService.published());
    }
}

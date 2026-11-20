package com.shorthand.controller.admin;

import com.shorthand.common.Result;
import com.shorthand.service.IAdminDashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-总览")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final IAdminDashboardService adminDashboardService;

    @GetMapping
    public Result overview() {
        return Result.ok(adminDashboardService.overview());
    }
}

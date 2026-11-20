package com.shorthand.controller;

import com.shorthand.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "用户-版本")
@RestController
@RequestMapping("/api/app")
public class AppVersionController {
    @Value("${app.latest-version:0.4.0}")
    private String latestVersion;

    @Value("${app.download-url:https://github.com/WxhnQuXihe/Quick-Word-Memorization/releases/latest}")
    private String downloadUrl;

    @Value("${app.update-notes:修复问题并带来新的主页体验}")
    private String notes;

    @GetMapping("/version")
    public Result version() {
        return Result.ok(Map.of(
                "latestVersion", latestVersion,
                "downloadUrl", downloadUrl,
                "notes", notes
        ));
    }
}

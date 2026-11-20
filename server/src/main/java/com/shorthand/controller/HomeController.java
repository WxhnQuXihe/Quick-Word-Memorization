package com.shorthand.controller;

import com.shorthand.common.Result;
import com.shorthand.service.IWordbookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户-首页")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeController {
    private final IWordbookService wordbookService;

    @GetMapping("/home")
    public Result home(@RequestAttribute("userId") Long userId) {
        return Result.ok(wordbookService.home(userId));
    }
}

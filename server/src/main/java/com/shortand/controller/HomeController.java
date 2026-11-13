package com.shortand.controller;

import com.shortand.common.Result;
import com.shortand.service.IWordbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

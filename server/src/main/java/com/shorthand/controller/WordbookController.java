package com.shorthand.controller;

import com.shorthand.common.Result;
import com.shorthand.service.IWordbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wordbooks")
@RequiredArgsConstructor
public class WordbookController {
    private final IWordbookService wordbookService;

    @GetMapping
    public Result list(@RequestAttribute("userId") Long userId) {
        return Result.ok(wordbookService.listPublished(userId));
    }

    @GetMapping("/mine")
    public Result mine(@RequestAttribute("userId") Long userId) {
        return Result.ok(wordbookService.mine(userId));
    }

    @PostMapping("/{id}/select")
    public Result select(@RequestAttribute("userId") Long userId, @PathVariable("id") Long id) {
        return Result.ok(wordbookService.select(userId, id));
    }
}

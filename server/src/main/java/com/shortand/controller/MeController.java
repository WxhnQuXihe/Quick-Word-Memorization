package com.shortand.controller;

import com.shortand.common.Result;
import com.shortand.service.IStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {
    private final IStudyService studyService;

    @GetMapping("/favorites")
    public Result favorites(@RequestAttribute("userId") Long userId,
                            @RequestParam(defaultValue = "1") long pageNo,
                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(studyService.favorites(userId, pageNo, pageSize));
    }

    @DeleteMapping("/favorites/{wordId}")
    public Result removeFavorite(@RequestAttribute("userId") Long userId, @PathVariable Long wordId) {
        return Result.ok(studyService.removeFavorite(userId, wordId));
    }

    @GetMapping("/known")
    public Result known(@RequestAttribute("userId") Long userId,
                        @RequestParam(defaultValue = "1") long pageNo,
                        @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(studyService.known(userId, pageNo, pageSize));
    }

    @DeleteMapping("/known/{wordId}")
    public Result removeKnown(@RequestAttribute("userId") Long userId, @PathVariable Long wordId) {
        return Result.ok(studyService.removeKnown(userId, wordId));
    }
}

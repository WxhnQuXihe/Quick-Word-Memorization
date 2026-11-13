package com.shortand.controller;

import com.shortand.common.Result;
import com.shortand.service.IStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {
    private final IStudyService studyService;

    @GetMapping("/current")
    public Result current(@RequestAttribute("userId") Long userId, @RequestParam Long wordbookId) {
        return Result.ok(studyService.current(userId, wordbookId));
    }

    @PostMapping("/group/{id}/next")
    public Result next(@RequestAttribute("userId") Long userId, @PathVariable("id") Long id) {
        return Result.ok(studyService.next(userId, id));
    }

    @PostMapping("/group/{id}/known/{wordId}")
    public Result known(@RequestAttribute("userId") Long userId,
                        @PathVariable("id") Long id,
                        @PathVariable("wordId") Long wordId) {
        return Result.ok(studyService.markKnown(userId, id, wordId));
    }

    @PostMapping("/group/{id}/favorite/{wordId}")
    public Result favorite(@RequestAttribute("userId") Long userId,
                           @PathVariable("id") Long id,
                           @PathVariable("wordId") Long wordId) {
        return Result.ok(studyService.toggleFavorite(userId, id, wordId));
    }
}

package com.shorthand.controller.admin;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.AnnouncementBo;
import com.shorthand.service.IAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {
    private final IAnnouncementService announcementService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") long pageNo,
                       @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(announcementService.list(pageNo, pageSize));
    }

    @PostMapping
    public Result create(@Valid @RequestBody AnnouncementBo bo) {
        return Result.ok(announcementService.save(bo));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody AnnouncementBo bo) {
        return Result.ok(announcementService.update(id, bo));
    }

    @PostMapping("/{id}/status")
    public Result status(@PathVariable Long id, @RequestParam Integer status) {
        announcementService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.ok();
    }
}

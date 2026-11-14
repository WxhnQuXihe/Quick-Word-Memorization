package com.shorthand.controller.admin;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.WordbookSaveBo;
import com.shorthand.service.IAdminWordbookService;
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
@RequestMapping("/api/admin/wordbooks")
@RequiredArgsConstructor
public class AdminWordbookController {
    private final IAdminWordbookService adminWordbookService;

    @GetMapping
    public Result list() {
        return Result.ok(adminWordbookService.list());
    }

    @PostMapping
    public Result create(@Valid @RequestBody WordbookSaveBo bo) {
        return Result.ok(adminWordbookService.create(bo));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody WordbookSaveBo bo) {
        return Result.ok(adminWordbookService.update(id, bo));
    }

    @PostMapping("/{id}/status")
    public Result status(@PathVariable Long id, @RequestParam Integer status) {
        adminWordbookService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}/words")
    public Result clearWords(@PathVariable Long id) {
        adminWordbookService.clearWords(id);
        return Result.ok();
    }
}

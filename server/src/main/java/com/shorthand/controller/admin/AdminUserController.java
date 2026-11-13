package com.shorthand.controller.admin;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.CreateUserBo;
import com.shorthand.domain.bo.ResetPasswordBo;
import com.shorthand.service.IAdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final IAdminUserService adminUserService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") long pageNo,
                       @RequestParam(defaultValue = "20") long pageSize,
                       @RequestParam(required = false) String keyword) {
        return Result.ok(adminUserService.list(pageNo, pageSize, keyword));
    }

    @PostMapping
    public Result create(@Valid @RequestBody CreateUserBo bo) {
        adminUserService.create(bo);
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    public Result resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordBo bo) {
        adminUserService.resetPassword(id, bo.getPassword());
        return Result.ok();
    }

    @PatchMapping("/{id}/status")
    public Result status(@PathVariable Long id, @RequestParam Integer status) {
        adminUserService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Result.ok();
    }
}

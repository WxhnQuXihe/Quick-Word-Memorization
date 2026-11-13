package com.shortand.controller.admin;

import com.shortand.common.Result;
import com.shortand.domain.bo.LoginBo;
import com.shortand.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final IAuthService authService;

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginBo bo) {
        return Result.ok(authService.loginAdmin(bo));
    }
}

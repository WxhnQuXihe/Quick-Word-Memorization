package com.shorthand.controller;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.LoginBo;
import com.shorthand.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService authService;

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginBo bo) {
        return Result.ok(authService.loginApp(bo));
    }
}

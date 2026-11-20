package com.shorthand.controller;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.LoginBo;
import com.shorthand.service.IAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户-认证")
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

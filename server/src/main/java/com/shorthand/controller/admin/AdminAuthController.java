package com.shorthand.controller.admin;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.LoginBo;
import com.shorthand.domain.bo.PasswordChangeBo;
import com.shorthand.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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

    @PostMapping("/password")
    public Result changePassword(@RequestAttribute("adminId") Long adminId,
                                 @Valid @RequestBody PasswordChangeBo bo) {
        authService.changeAdminPassword(adminId, bo.getOldPassword(), bo.getNewPassword());
        return Result.ok();
    }
}

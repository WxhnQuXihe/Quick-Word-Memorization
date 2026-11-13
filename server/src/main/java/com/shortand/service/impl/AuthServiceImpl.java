package com.shortand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shortand.common.BizException;
import com.shortand.config.TokenStore;
import com.shortand.domain.AdminUser;
import com.shortand.domain.AppUser;
import com.shortand.domain.bo.LoginBo;
import com.shortand.domain.vo.LoginVo;
import com.shortand.mapper.AdminUserMapper;
import com.shortand.mapper.AppUserMapper;
import com.shortand.service.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {
    private final AppUserMapper appUserMapper;
    private final AdminUserMapper adminUserMapper;
    private final TokenStore tokenStore;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVo loginApp(LoginBo bo) {
        AppUser user = appUserMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, bo.getUsername()));
        if (user == null || !passwordEncoder.matches(bo.getPassword(), user.getPassword())) {
            throw new BizException(401, "账号或密码不对");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }
        user.setLastLoginTime(LocalDateTime.now());
        appUserMapper.updateById(user);

        LoginVo vo = new LoginVo();
        vo.setToken(tokenStore.issueUser(user.getId()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    public LoginVo loginAdmin(LoginBo bo) {
        AdminUser admin = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, bo.getUsername()));
        if (admin == null || !passwordEncoder.matches(bo.getPassword(), admin.getPassword())) {
            throw new BizException(401, "账号或密码不对");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(403, "管理员账号已禁用");
        }
        admin.setLastLoginTime(LocalDateTime.now());
        adminUserMapper.updateById(admin);

        LoginVo vo = new LoginVo();
        vo.setToken(tokenStore.issueAdmin(admin.getId()));
        vo.setUserId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getName());
        return vo;
    }
}

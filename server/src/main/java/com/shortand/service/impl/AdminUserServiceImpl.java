package com.shortand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortand.common.BizException;
import com.shortand.common.PageResult;
import com.shortand.domain.AppUser;
import com.shortand.domain.bo.CreateUserBo;
import com.shortand.domain.vo.UserVo;
import com.shortand.mapper.AppUserMapper;
import com.shortand.service.IAdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {
    private final AppUserMapper appUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public PageResult list(long pageNo, long pageSize, String keyword) {
        Page<AppUser> page = appUserMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<AppUser>lambdaQuery()
                        .like(StringUtils.hasText(keyword), AppUser::getUsername, keyword)
                        .or(StringUtils.hasText(keyword))
                        .like(StringUtils.hasText(keyword), AppUser::getNickname, keyword)
                        .orderByDesc(AppUser::getId));
        return PageResult.of(page.getRecords().stream().map(this::toVo).toList(),
                page.getTotal(), pageNo, pageSize);
    }

    @Override
    public void create(CreateUserBo bo) {
        Long count = appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, bo.getUsername()));
        if (count != null && count > 0) {
            throw new BizException("用户名已存在");
        }
        AppUser user = new AppUser();
        user.setUsername(bo.getUsername());
        user.setPassword(passwordEncoder.encode(bo.getPassword()));
        user.setNickname(StringUtils.hasText(bo.getNickname()) ? bo.getNickname() : bo.getUsername());
        user.setStatus(1);
        user.setTheme("system");
        user.setReminderEnabled(0);
        appUserMapper.insert(user);
    }

    @Override
    public void resetPassword(Long userId, String password) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");
        user.setPassword(passwordEncoder.encode(password));
        appUserMapper.updateById(user);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");
        user.setStatus(status == null ? 0 : status);
        appUserMapper.updateById(user);
    }

    private UserVo toVo(AppUser user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }
}

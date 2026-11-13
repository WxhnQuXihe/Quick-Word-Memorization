package com.shorthand.service;

import com.shorthand.common.PageResult;
import com.shorthand.domain.bo.CreateUserBo;

public interface IAdminUserService {
    PageResult list(long pageNo, long pageSize, String keyword);

    void create(CreateUserBo bo);

    void resetPassword(Long userId, String password);

    void updateStatus(Long userId, Integer status);
}

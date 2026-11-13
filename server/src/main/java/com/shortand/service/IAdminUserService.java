package com.shortand.service;

import com.shortand.common.PageResult;
import com.shortand.domain.bo.CreateUserBo;

public interface IAdminUserService {
    PageResult list(long pageNo, long pageSize, String keyword);

    void create(CreateUserBo bo);

    void resetPassword(Long userId, String password);

    void updateStatus(Long userId, Integer status);
}

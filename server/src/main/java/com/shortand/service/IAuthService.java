package com.shortand.service;

import com.shortand.domain.bo.LoginBo;
import com.shortand.domain.vo.LoginVo;

public interface IAuthService {
    LoginVo loginApp(LoginBo bo);

    LoginVo loginAdmin(LoginBo bo);
}

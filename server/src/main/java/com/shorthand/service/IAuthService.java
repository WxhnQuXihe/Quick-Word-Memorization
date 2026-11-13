package com.shorthand.service;

import com.shorthand.domain.bo.LoginBo;
import com.shorthand.domain.vo.LoginVo;

public interface IAuthService {
    LoginVo loginApp(LoginBo bo);

    LoginVo loginAdmin(LoginBo bo);
}

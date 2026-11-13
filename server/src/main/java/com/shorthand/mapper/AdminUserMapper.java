package com.shorthand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorthand.domain.AdminUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}

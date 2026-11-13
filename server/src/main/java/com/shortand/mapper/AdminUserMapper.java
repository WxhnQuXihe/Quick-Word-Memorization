package com.shortand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortand.domain.AdminUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}

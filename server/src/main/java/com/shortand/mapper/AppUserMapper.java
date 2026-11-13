package com.shortand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortand.domain.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}

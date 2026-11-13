package com.shorthand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorthand.domain.Feedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}

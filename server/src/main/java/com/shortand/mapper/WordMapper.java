package com.shortand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortand.domain.Word;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordMapper extends BaseMapper<Word> {
}

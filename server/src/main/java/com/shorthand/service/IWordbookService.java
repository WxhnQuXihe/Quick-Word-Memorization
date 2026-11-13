package com.shorthand.service;

import com.shorthand.domain.vo.HomeVo;
import com.shorthand.domain.vo.WordbookVo;

import java.util.List;

public interface IWordbookService {
    List<WordbookVo> listPublished(Long userId);

    List<WordbookVo> mine(Long userId);

    WordbookVo select(Long userId, Long wordbookId);

    HomeVo home(Long userId);
}

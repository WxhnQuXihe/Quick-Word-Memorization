package com.shortand.service;

import com.shortand.domain.vo.HomeVo;
import com.shortand.domain.vo.WordbookVo;

import java.util.List;

public interface IWordbookService {
    List<WordbookVo> listPublished(Long userId);

    List<WordbookVo> mine(Long userId);

    WordbookVo select(Long userId, Long wordbookId);

    HomeVo home(Long userId);
}

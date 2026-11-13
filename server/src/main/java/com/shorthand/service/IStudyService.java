package com.shorthand.service;

import com.shorthand.common.PageResult;
import com.shorthand.domain.vo.GroupVo;
import com.shorthand.domain.vo.WordVo;

public interface IStudyService {
    GroupVo current(Long userId, Long wordbookId);

    GroupVo next(Long userId, Long groupId);

    WordVo markKnown(Long userId, Long groupId, Long wordId);

    boolean toggleFavorite(Long userId, Long groupId, Long wordId);

    PageResult favorites(Long userId, long pageNo, long pageSize);

    PageResult known(Long userId, long pageNo, long pageSize);

    boolean removeFavorite(Long userId, Long wordId);

    boolean removeKnown(Long userId, Long wordId);
}

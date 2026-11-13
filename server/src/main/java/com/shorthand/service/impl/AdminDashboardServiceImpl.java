package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shorthand.domain.AppUser;
import com.shorthand.domain.Feedback;
import com.shorthand.domain.UserWord;
import com.shorthand.domain.Wordbook;
import com.shorthand.domain.vo.AdminDashboardVo;
import com.shorthand.domain.vo.UserVo;
import com.shorthand.domain.vo.WordbookVo;
import com.shorthand.mapper.AppUserMapper;
import com.shorthand.mapper.FeedbackMapper;
import com.shorthand.mapper.UserWordMapper;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookMapper;
import com.shorthand.service.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements IAdminDashboardService {
    private final WordbookMapper wordbookMapper;
    private final WordMapper wordMapper;
    private final AppUserMapper appUserMapper;
    private final UserWordMapper userWordMapper;
    private final FeedbackMapper feedbackMapper;

    @Override
    public AdminDashboardVo overview() {
        AdminDashboardVo vo = new AdminDashboardVo();
        vo.setBookCount(wordbookMapper.selectCount(null));
        vo.setWordCount(wordMapper.selectCount(null));
        vo.setUserCount(appUserMapper.selectCount(null));
        vo.setFavoriteCount(userWordMapper.selectCount(Wrappers.<UserWord>lambdaQuery()
                .eq(UserWord::getIsFavorite, 1)));
        vo.setKnownCount(userWordMapper.selectCount(Wrappers.<UserWord>lambdaQuery()
                .eq(UserWord::getIsKnown, 1)));
        vo.setOpenFeedbackCount(feedbackMapper.selectCount(Wrappers.<Feedback>lambdaQuery()
                .eq(Feedback::getStatus, 0)));
        vo.setBooks(wordbookMapper.selectList(Wrappers.<Wordbook>lambdaQuery()
                        .orderByAsc(Wordbook::getSort).orderByDesc(Wordbook::getId))
                .stream().map(this::bookVo).toList());
        vo.setRecentUsers(appUserMapper.selectList(Wrappers.<AppUser>lambdaQuery()
                        .orderByDesc(AppUser::getId)).stream().limit(5).map(this::userVo).toList());
        return vo;
    }

    private WordbookVo bookVo(Wordbook book) {
        WordbookVo vo = new WordbookVo();
        vo.setId(book.getId());
        vo.setCode(book.getCode());
        vo.setName(book.getName());
        vo.setWordCount(book.getWordCount());
        vo.setStatus(book.getStatus());
        vo.setSort(book.getSort());
        vo.setDifficulty(book.getDifficulty());
        vo.setUpdateTime(book.getUpdateTime());
        return vo;
    }

    private UserVo userVo(AppUser user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }
}

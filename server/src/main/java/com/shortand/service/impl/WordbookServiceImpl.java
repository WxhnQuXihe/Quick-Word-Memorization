package com.shortand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shortand.common.BizException;
import com.shortand.domain.UserWord;
import com.shortand.domain.UserWordbook;
import com.shortand.domain.Wordbook;
import com.shortand.domain.vo.HomeVo;
import com.shortand.domain.vo.WordbookVo;
import com.shortand.mapper.UserWordMapper;
import com.shortand.mapper.UserWordbookMapper;
import com.shortand.mapper.WordbookMapper;
import com.shortand.service.IWordbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WordbookServiceImpl implements IWordbookService {
    private final WordbookMapper wordbookMapper;
    private final UserWordbookMapper userWordbookMapper;
    private final UserWordMapper userWordMapper;

    @Override
    public List<WordbookVo> listPublished(Long userId) {
        List<Wordbook> books = wordbookMapper.selectList(Wrappers.<Wordbook>lambdaQuery()
                .eq(Wordbook::getStatus, 1)
                .orderByAsc(Wordbook::getSort)
                .orderByAsc(Wordbook::getId));
        return books.stream().map(book -> toVo(book, findUserBook(userId, book.getId()))).toList();
    }

    @Override
    public List<WordbookVo> mine(Long userId) {
        List<UserWordbook> rows = userWordbookMapper.selectList(Wrappers.<UserWordbook>lambdaQuery()
                .eq(UserWordbook::getUserId, userId)
                .orderByDesc(UserWordbook::getLastStudyTime));
        List<WordbookVo> result = new ArrayList<>();
        for (UserWordbook row : rows) {
            Wordbook book = wordbookMapper.selectById(row.getWordbookId());
            if (book != null) {
                result.add(toVo(book, row));
            }
        }
        return result;
    }

    @Override
    public WordbookVo select(Long userId, Long wordbookId) {
        Wordbook book = wordbookMapper.selectById(wordbookId);
        if (book == null || book.getStatus() == null || book.getStatus() != 1) {
            throw new BizException("词书不存在或未上架");
        }
        UserWordbook row = findUserBook(userId, wordbookId);
        if (row == null) {
            row = new UserWordbook();
            row.setUserId(userId);
            row.setWordbookId(wordbookId);
            row.setCurrentRoundNo(1);
            row.setLastStudyTime(LocalDateTime.now());
            userWordbookMapper.insert(row);
        } else {
            row.setLastStudyTime(LocalDateTime.now());
            userWordbookMapper.updateById(row);
        }
        return toVo(book, row);
    }

    @Override
    public HomeVo home(Long userId) {
        HomeVo vo = new HomeVo();
        UserWordbook latest = userWordbookMapper.selectOne(Wrappers.<UserWordbook>lambdaQuery()
                .eq(UserWordbook::getUserId, userId)
                .orderByDesc(UserWordbook::getLastStudyTime)
                .last("LIMIT 1"));
        if (latest != null) {
            Wordbook book = wordbookMapper.selectById(latest.getWordbookId());
            if (book != null) {
                vo.setCurrentBook(toVo(book, latest));
                vo.setLearnedCount(learnedCount(userId, book.getId()));
            }
        }
        vo.setFavoriteCount(userWordMapper.selectCount(Wrappers.<UserWord>lambdaQuery()
                .eq(UserWord::getUserId, userId)
                .eq(UserWord::getIsFavorite, 1)));
        vo.setKnownCount(userWordMapper.selectCount(Wrappers.<UserWord>lambdaQuery()
                .eq(UserWord::getUserId, userId)
                .eq(UserWord::getIsKnown, 1)));
        return vo;
    }

    private UserWordbook findUserBook(Long userId, Long wordbookId) {
        return userWordbookMapper.selectOne(Wrappers.<UserWordbook>lambdaQuery()
                .eq(UserWordbook::getUserId, userId)
                .eq(UserWordbook::getWordbookId, wordbookId)
                .last("LIMIT 1"));
    }

    private long learnedCount(Long userId, Long wordbookId) {
        return userWordMapper.selectCount(Wrappers.<UserWord>lambdaQuery()
                .eq(UserWord::getUserId, userId)
                .eq(UserWord::getFirstWordbookId, wordbookId)
                .gt(UserWord::getSeenCount, 0));
    }

    private WordbookVo toVo(Wordbook book, UserWordbook row) {
        WordbookVo vo = new WordbookVo();
        vo.setId(book.getId());
        vo.setCode(book.getCode());
        vo.setName(book.getName());
        vo.setDescription(book.getDescription());
        vo.setCover(book.getCover());
        vo.setDifficulty(book.getDifficulty());
        vo.setWordCount(book.getWordCount());
        vo.setStatus(book.getStatus());
        if (row != null) {
            vo.setCurrentRoundNo(row.getCurrentRoundNo());
            vo.setLastStudyTime(row.getLastStudyTime());
        }
        return vo;
    }
}

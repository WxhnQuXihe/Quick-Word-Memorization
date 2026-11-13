package com.shortand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shortand.common.BizException;
import com.shortand.domain.Wordbook;
import com.shortand.domain.WordbookWord;
import com.shortand.domain.bo.WordbookSaveBo;
import com.shortand.domain.vo.WordbookVo;
import com.shortand.mapper.WordbookMapper;
import com.shortand.mapper.WordbookWordMapper;
import com.shortand.service.IAdminWordbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminWordbookServiceImpl implements IAdminWordbookService {
    private final WordbookMapper wordbookMapper;
    private final WordbookWordMapper wordbookWordMapper;

    @Override
    public List<WordbookVo> list() {
        return wordbookMapper.selectList(Wrappers.<Wordbook>lambdaQuery()
                        .orderByAsc(Wordbook::getSort)
                        .orderByDesc(Wordbook::getId))
                .stream().map(this::toVo).toList();
    }

    @Override
    public WordbookVo create(WordbookSaveBo bo) {
        checkCode(bo.getCode(), null);
        Wordbook book = new Wordbook();
        apply(book, bo);
        book.setWordCount(0);
        book.setSort(bo.getSort() == null ? 100 : bo.getSort());
        book.setStatus(bo.getStatus() == null ? 0 : bo.getStatus());
        wordbookMapper.insert(book);
        return toVo(book);
    }

    @Override
    public WordbookVo update(Long id, WordbookSaveBo bo) {
        Wordbook book = wordbookMapper.selectById(id);
        if (book == null) throw new BizException("词书不存在");
        checkCode(bo.getCode(), id);
        apply(book, bo);
        if (bo.getSort() != null) book.setSort(bo.getSort());
        if (bo.getStatus() != null) book.setStatus(bo.getStatus());
        wordbookMapper.updateById(book);
        return toVo(book);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Wordbook book = wordbookMapper.selectById(id);
        if (book == null) throw new BizException("词书不存在");
        book.setStatus(status == null ? 0 : status);
        if (status != null && status == 1) {
            Long count = wordbookWordMapper.selectCount(Wrappers.<WordbookWord>lambdaQuery()
                    .eq(WordbookWord::getWordbookId, id));
            book.setWordCount(count == null ? 0 : count.intValue());
        }
        wordbookMapper.updateById(book);
    }

    private void apply(Wordbook book, WordbookSaveBo bo) {
        book.setCode(bo.getCode());
        book.setName(bo.getName());
        book.setDescription(bo.getDescription());
        book.setCover(bo.getCover());
        book.setDifficulty(bo.getDifficulty() == null ? 3 : bo.getDifficulty());
    }

    private void checkCode(String code, Long excludeId) {
        Long count = wordbookMapper.selectCount(Wrappers.<Wordbook>lambdaQuery()
                .eq(Wordbook::getCode, code)
                .ne(excludeId != null, Wordbook::getId, excludeId));
        if (count != null && count > 0) throw new BizException("词书编码已存在");
    }

    private WordbookVo toVo(Wordbook book) {
        WordbookVo vo = new WordbookVo();
        vo.setId(book.getId());
        vo.setCode(book.getCode());
        vo.setName(book.getName());
        vo.setDescription(book.getDescription());
        vo.setCover(book.getCover());
        vo.setDifficulty(book.getDifficulty());
        vo.setWordCount(book.getWordCount());
        vo.setStatus(book.getStatus());
        vo.setSort(book.getSort());
        vo.setUpdateTime(book.getUpdateTime());
        return vo;
    }
}

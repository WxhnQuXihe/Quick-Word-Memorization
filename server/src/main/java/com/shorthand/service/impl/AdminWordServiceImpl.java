package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorthand.common.BizException;
import com.shorthand.common.PageResult;
import com.shorthand.domain.Word;
import com.shorthand.domain.bo.WordSaveBo;
import com.shorthand.domain.vo.AdminWordVo;
import com.shorthand.mapper.WordMapper;
import com.shorthand.service.IAdminWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminWordServiceImpl implements IAdminWordService {
    private final WordMapper wordMapper;

    @Override
    public PageResult list(long pageNo, long pageSize, String keyword) {
        Page<Word> page = wordMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<Word>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(Word::getWord, keyword)
                                .or()
                                .like(Word::getMeaning, keyword))
                        .orderByDesc(Word::getId));
        return PageResult.of(page.getRecords().stream().map(this::toVo).toList(),
                page.getTotal(), pageNo, pageSize);
    }

    @Override
    public Long create(WordSaveBo bo) {
        String normalized = normalize(bo.getWord());
        Long count = wordMapper.selectCount(Wrappers.<Word>lambdaQuery()
                .eq(Word::getNormalized, normalized));
        if (count != null && count > 0) throw new BizException("单词已存在");
        Word word = new Word();
        apply(word, bo);
        word.setNormalized(normalized);
        wordMapper.insert(word);
        return word.getId();
    }

    @Override
    public void update(Long id, WordSaveBo bo) {
        Word word = wordMapper.selectById(id);
        if (word == null) throw new BizException("词条不存在");
        String normalized = normalize(bo.getWord());
        Long count = wordMapper.selectCount(Wrappers.<Word>lambdaQuery()
                .eq(Word::getNormalized, normalized)
                .ne(Word::getId, id));
        if (count != null && count > 0) throw new BizException("单词已存在");
        apply(word, bo);
        word.setNormalized(normalized);
        wordMapper.updateById(word);
    }

    @Override
    public void delete(Long id) {
        wordMapper.deleteById(id);
    }

    private void apply(Word word, WordSaveBo bo) {
        word.setWord(bo.getWord().trim());
        word.setPhoneticUk(bo.getPhoneticUk());
        word.setPhoneticUs(bo.getPhoneticUs());
        word.setPos(bo.getPos());
        word.setMeaning(bo.getMeaning());
        word.setExampleEn(bo.getExampleEn());
        word.setExampleCn(bo.getExampleCn());
    }

    private String normalize(String word) {
        return word == null ? "" : word.trim().toLowerCase();
    }

    private AdminWordVo toVo(Word word) {
        AdminWordVo vo = new AdminWordVo();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setNormalized(word.getNormalized());
        vo.setPhoneticUk(word.getPhoneticUk());
        vo.setPhoneticUs(word.getPhoneticUs());
        vo.setPos(word.getPos());
        vo.setMeaning(word.getMeaning());
        vo.setExampleEn(word.getExampleEn());
        vo.setExampleCn(word.getExampleCn());
        vo.setUpdateTime(word.getUpdateTime());
        return vo;
    }
}

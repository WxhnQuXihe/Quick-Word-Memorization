package com.shorthand.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shorthand.common.BizException;
import com.shorthand.domain.Word;
import com.shorthand.domain.Wordbook;
import com.shorthand.domain.WordbookWord;
import com.shorthand.domain.bo.WordbookSaveBo;
import com.shorthand.domain.vo.WordImportVo;
import com.shorthand.domain.vo.WordbookVo;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookMapper;
import com.shorthand.mapper.WordbookWordMapper;
import com.shorthand.service.IAdminWordbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminWordbookServiceImpl implements IAdminWordbookService {
    private final WordbookMapper wordbookMapper;
    private final WordbookWordMapper wordbookWordMapper;
    private final WordMapper wordMapper;

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

    @Override
    @Transactional
    public void clearWords(Long id) {
        Wordbook book = wordbookMapper.selectById(id);
        if (book == null) throw new BizException("词书不存在");
        wordbookWordMapper.delete(Wrappers.<WordbookWord>lambdaQuery()
                .eq(WordbookWord::getWordbookId, id));
        book.setWordCount(0);
        wordbookMapper.updateById(book);
    }

    @Override
    public byte[] exportExcel(Long id) {
        Wordbook book = wordbookMapper.selectById(id);
        if (book == null) throw new BizException("词书不存在");
        List<WordbookWord> relations = wordbookWordMapper.selectList(Wrappers.<WordbookWord>lambdaQuery()
                .eq(WordbookWord::getWordbookId, id)
                .orderByAsc(WordbookWord::getSeq));
        List<Long> wordIds = relations.stream().map(WordbookWord::getWordId).filter(java.util.Objects::nonNull).toList();
        Map<Long, Word> words = wordIds.isEmpty() ? Map.of() : wordMapper.selectBatchIds(wordIds).stream()
                .collect(Collectors.toMap(Word::getId, w -> w, (a, b) -> a));

        List<WordImportVo> rows = new ArrayList<>();
        for (WordbookWord relation : relations) {
            Word word = words.get(relation.getWordId());
            if (word == null) continue;
            WordImportVo row = new WordImportVo();
            row.setSeq(relation.getSeq());
            row.setWord(word.getWord());
            row.setPhoneticUk(word.getPhoneticUk());
            row.setPhoneticUs(word.getPhoneticUs());
            row.setPos(word.getPos());
            row.setMeaning(word.getMeaning());
            row.setExampleEn(word.getExampleEn());
            row.setExampleCn(word.getExampleCn());
            rows.add(row);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, WordImportVo.class).sheet("单词").doWrite(rows);
        return output.toByteArray();
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

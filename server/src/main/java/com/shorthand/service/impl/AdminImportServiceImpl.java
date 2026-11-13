package com.shorthand.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorthand.common.BizException;
import com.shorthand.common.PageResult;
import com.shorthand.domain.ImportLog;
import com.shorthand.domain.Word;
import com.shorthand.domain.Wordbook;
import com.shorthand.domain.WordbookWord;
import com.shorthand.domain.vo.ImportResultVo;
import com.shorthand.domain.vo.WordImportVo;
import com.shorthand.listener.WordImportListener;
import com.shorthand.mapper.ImportLogMapper;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookMapper;
import com.shorthand.mapper.WordbookWordMapper;
import com.shorthand.service.IAdminImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminImportServiceImpl implements IAdminImportService {
    private final WordbookMapper wordbookMapper;
    private final WordMapper wordMapper;
    private final WordbookWordMapper wordbookWordMapper;
    private final ImportLogMapper importLogMapper;

    @Override
    public ImportResultVo importExcel(MultipartFile file, Long wordbookId, Long adminId) {
        Wordbook book = wordbookMapper.selectById(wordbookId);
        if (book == null) throw new BizException("词书不存在");
        if (file == null || file.isEmpty()) throw new BizException("请选择 Excel 文件");

        ImportLog log = new ImportLog();
        log.setWordbookId(wordbookId);
        log.setFileName(file.getOriginalFilename());
        log.setStatus(0);
        log.setAdminId(adminId);
        importLogMapper.insert(log);

        ImportResultVo result = new ImportResultVo();
        try {
            WordImportListener listener = new WordImportListener(batch -> importBatch(wordbookId, batch, result));
            EasyExcel.read(file.getInputStream(), WordImportVo.class, listener).sheet().doRead();
            result.setTotalRows(listener.getTotal());
            if (result.getSuccessRows() == 0 && result.getFailRows() > 0) {
                log.setStatus(3);
            } else if (result.getFailRows() > 0) {
                log.setStatus(2);
            } else {
                log.setStatus(1);
            }
        } catch (Exception e) {
            log.setStatus(3);
            result.getErrors().add("导入失败：" + e.getMessage());
        } finally {
            refreshCount(wordbookId);
            log.setTotalRows(result.getTotalRows());
            log.setSuccessRows(result.getSuccessRows());
            log.setFailRows(result.getFailRows());
            log.setFinishTime(LocalDateTime.now());
            importLogMapper.updateById(log);
        }
        return result;
    }

    @Override
    public PageResult logs(long pageNo, long pageSize) {
        Page<ImportLog> page = importLogMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<ImportLog>lambdaQuery().orderByDesc(ImportLog::getId));
        return PageResult.of(page.getRecords(), page.getTotal(), pageNo, pageSize);
    }

    private void importBatch(Long wordbookId, List<WordImportVo> rows, ImportResultVo result) {
        List<String> normalized = rows.stream()
                .map(WordImportVo::getWord)
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .distinct()
                .toList();
        Map<String, Word> existing = normalized.isEmpty() ? new HashMap<>()
                : wordMapper.selectList(Wrappers.<Word>lambdaQuery()
                        .in(Word::getNormalized, normalized))
                .stream().collect(Collectors.toMap(Word::getNormalized, w -> w, (a, b) -> a));

        int seq = nextSeq(wordbookId);
        for (WordImportVo row : rows) {
            if (!StringUtils.hasText(row.getWord()) || !StringUtils.hasText(row.getMeaning())) {
                result.setFailRows(result.getFailRows() + 1);
                addError(result, row, "单词或释义为空");
                continue;
            }
            try {
                String norm = normalize(row.getWord());
                Word word = existing.get(norm);
                if (word == null) {
                    word = new Word();
                    word.setWord(row.getWord().trim());
                    word.setNormalized(norm);
                    word.setPhoneticUk(row.getPhoneticUk());
                    word.setPhoneticUs(row.getPhoneticUs());
                    word.setPos(row.getPos());
                    word.setMeaning(row.getMeaning().trim());
                    word.setExampleEn(row.getExampleEn());
                    word.setExampleCn(row.getExampleCn());
                    wordMapper.insert(word);
                    existing.put(norm, word);
                }
                boolean mapped = wordbookWordMapper.selectCount(Wrappers.<WordbookWord>lambdaQuery()
                        .eq(WordbookWord::getWordbookId, wordbookId)
                        .eq(WordbookWord::getWordId, word.getId())) > 0;
                if (!mapped) {
                    WordbookWord relation = new WordbookWord();
                    relation.setWordbookId(wordbookId);
                    relation.setWordId(word.getId());
                    relation.setSeq(row.getSeq() != null && row.getSeq() > 0 ? row.getSeq() : seq);
                    wordbookWordMapper.insert(relation);
                    seq = Math.max(seq, relation.getSeq()) + 1;
                }
                result.setSuccessRows(result.getSuccessRows() + 1);
            } catch (Exception e) {
                result.setFailRows(result.getFailRows() + 1);
                addError(result, row, e.getMessage());
            }
        }
    }

    private void refreshCount(Long wordbookId) {
        Wordbook book = wordbookMapper.selectById(wordbookId);
        if (book == null) return;
        Long count = wordbookWordMapper.selectCount(Wrappers.<WordbookWord>lambdaQuery()
                .eq(WordbookWord::getWordbookId, wordbookId));
        book.setWordCount(count == null ? 0 : count.intValue());
        wordbookMapper.updateById(book);
    }

    private int nextSeq(Long wordbookId) {
        List<WordbookWord> rows = wordbookWordMapper.selectList(Wrappers.<WordbookWord>lambdaQuery()
                .eq(WordbookWord::getWordbookId, wordbookId));
        return rows.stream().map(WordbookWord::getSeq).filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 1;
    }

    private void addError(ImportResultVo result, WordImportVo row, String message) {
        if (result.getErrors().size() >= 100) return;
        List<String> errors = new ArrayList<>(result.getErrors());
        errors.add("第 " + (row.getRowNum() == null ? "?" : row.getRowNum()) + " 行：" + message);
        result.setErrors(errors);
    }

    private String normalize(String word) {
        return word == null ? "" : word.trim().toLowerCase();
    }
}

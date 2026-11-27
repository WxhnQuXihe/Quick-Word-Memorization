package com.shorthand.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorthand.common.BizException;
import com.shorthand.common.PageResult;
import com.shorthand.common.WordTextUtils;
import com.shorthand.domain.ImportLog;
import com.shorthand.domain.Sense;
import com.shorthand.domain.Word;
import com.shorthand.domain.WordEntry;
import com.shorthand.domain.WordEntrySense;
import com.shorthand.domain.Wordbook;
import com.shorthand.domain.WordbookWord;
import com.shorthand.domain.vo.ImportResultVo;
import com.shorthand.domain.vo.WordImportVo;
import com.shorthand.listener.WordImportListener;
import com.shorthand.mapper.ImportLogMapper;
import com.shorthand.mapper.SenseMapper;
import com.shorthand.mapper.WordEntryMapper;
import com.shorthand.mapper.WordEntrySenseMapper;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookMapper;
import com.shorthand.mapper.WordbookWordMapper;
import com.shorthand.service.IAdminImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminImportServiceImpl implements IAdminImportService {
    private final WordbookMapper wordbookMapper;
    private final WordMapper wordMapper;
    private final WordbookWordMapper wordbookWordMapper;
    private final WordEntryMapper wordEntryMapper;
    private final SenseMapper senseMapper;
    private final WordEntrySenseMapper wordEntrySenseMapper;
    private final ImportLogMapper importLogMapper;
    private final TransactionTemplate transactionTemplate;

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
            List<WordImportVo> allRows = new ArrayList<>();
            WordImportListener listener = new WordImportListener(allRows::addAll);
            EasyExcel.read(file.getInputStream(), WordImportVo.class, listener).sheet().doRead();
            result.setTotalRows(listener.getTotal());

            List<PreparedRow> prepared = validateAndMerge(allRows, result);
            if (result.getFailRows() > 0) {
                log.setStatus(3);
                return result;
            }
            transactionTemplate.executeWithoutResult(status -> importPrepared(wordbookId, prepared));
            result.setSuccessRows(prepared.size());
            log.setStatus(1);
        } catch (Exception e) {
            log.setStatus(3);
            addError(result, "导入失败：" + e.getMessage());
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

    /**
     * 先完整校验，再一次性写入。缺词性/空释义的文件整批拒绝，不做半批导入。
     */
    private List<PreparedRow> validateAndMerge(List<WordImportVo> rows, ImportResultVo result) {
        Map<String, PreparedRow> merged = new LinkedHashMap<>();
        int rowErrors = 0;
        for (WordImportVo row : rows) {
            if (!WordTextUtils.hasText(row.getWord()) || !WordTextUtils.hasText(row.getMeaning())) {
                rowErrors++;
                addError(result, row, "单词或释义为空");
                continue;
            }
            try {
                String headword = WordTextUtils.headwordKey(row.getWord());
                String normalized = WordTextUtils.normalizeWord(row.getWord());
                String pronKey = WordTextUtils.pronunciationKey(row.getPhoneticUs(), row.getPhoneticUk());
                String identity = normalized + "\u0000" + pronKey + "\u0000" + headword;
                PreparedRow prepared = merged.get(identity);
                if (prepared == null) {
                    prepared = new PreparedRow(row, headword, normalized, pronKey);
                    merged.put(identity, prepared);
                }
                prepared.addSenses(WordTextUtils.parseMeaningLines(row.getMeaning()));
            } catch (Exception e) {
                rowErrors++;
                addError(result, row, e.getMessage());
            }
        }
        result.setFailRows(rowErrors);
        return new ArrayList<>(merged.values());
    }

    private void importPrepared(Long wordbookId, List<PreparedRow> rows) {
        int seq = nextSeq(wordbookId);
        for (PreparedRow row : rows) {
            Word word = findOrCreateWord(row);
            List<WordTextUtils.ParsedSense> parsedSenses = new ArrayList<>(row.senses.values());
            List<Long> senseIds = new ArrayList<>();
            for (WordTextUtils.ParsedSense parsed : parsedSenses) {
                Sense sense = senseMapper.selectOne(Wrappers.<Sense>lambdaQuery()
                        .eq(Sense::getWordId, word.getId())
                        .eq(Sense::getPosKey, parsed.posKey())
                        .eq(Sense::getMeaningKey, parsed.meaningKey())
                        .last("LIMIT 1"));
                if (sense == null) {
                    sense = new Sense();
                    sense.setWordId(word.getId());
                    sense.setPos(parsed.pos());
                    sense.setPosKey(parsed.posKey());
                    sense.setMeaning(parsed.meaning());
                    sense.setMeaningKey(parsed.meaningKey());
                    senseMapper.insert(sense);
                }
                senseIds.add(sense.getId());
            }

            String entryHash = WordTextUtils.entryHash(senseIds);
            WordEntry entry = wordEntryMapper.selectOne(Wrappers.<WordEntry>lambdaQuery()
                    .eq(WordEntry::getWordId, word.getId())
                    .eq(WordEntry::getEntryHash, entryHash)
                    .last("LIMIT 1"));
            if (entry == null) {
                entry = new WordEntry();
                entry.setWordId(word.getId());
                entry.setEntryHash(entryHash);
                wordEntryMapper.insert(entry);
            }

            for (int i = 0; i < senseIds.size(); i++) {
                Long senseId = senseIds.get(i);
                WordEntrySense link = wordEntrySenseMapper.selectOne(Wrappers.<WordEntrySense>lambdaQuery()
                        .eq(WordEntrySense::getEntryId, entry.getId())
                        .eq(WordEntrySense::getSenseId, senseId)
                        .last("LIMIT 1"));
                if (link == null) {
                    link = new WordEntrySense();
                    link.setEntryId(entry.getId());
                    link.setSenseId(senseId);
                    link.setSort(i);
                    link.setDisplayMeaning(parsedSenses.get(i).meaning());
                    wordEntrySenseMapper.insert(link);
                }
            }

            WordbookWord relation = wordbookWordMapper.selectOne(Wrappers.<WordbookWord>lambdaQuery()
                    .eq(WordbookWord::getWordbookId, wordbookId)
                    .eq(WordbookWord::getWordId, word.getId())
                    .last("LIMIT 1"));
            if (relation == null) {
                relation = new WordbookWord();
                relation.setWordbookId(wordbookId);
                relation.setWordId(word.getId());
                relation.setEntryId(entry.getId());
                relation.setSeq(row.seq != null && row.seq > 0 ? row.seq : seq);
                wordbookWordMapper.insert(relation);
                seq = Math.max(seq, relation.getSeq()) + 1;
            } else if (relation.getEntryId() == null) {
                relation.setEntryId(entry.getId());
                wordbookWordMapper.updateById(relation);
            } else if (!relation.getEntryId().equals(entry.getId())) {
                throw new BizException("词书内同词条出现多个义项集合：" + row.word);
            }

            // 兼容旧后台词条页：保留聚合释义与词性，实际学习展示读取 word_entry_sense。
            String aggregate = row.senses.values().stream()
                    .map(s -> s.pos() + " " + s.meaning())
                    .reduce((a, b) -> a + "\n" + b).orElse("");
            word.setMeaning(aggregate);
            word.setPos(row.senses.values().stream().findFirst().map(WordTextUtils.ParsedSense::pos).orElse(""));
            if (!WordTextUtils.hasText(word.getExampleEn())) word.setExampleEn(row.exampleEn);
            if (!WordTextUtils.hasText(word.getExampleCn())) word.setExampleCn(row.exampleCn);
            wordMapper.updateById(word);
        }
    }

    private Word findOrCreateWord(PreparedRow row) {
        Word exact = wordMapper.selectOne(Wrappers.<Word>lambdaQuery()
                .eq(Word::getNormalized, row.normalized)
                .eq(Word::getHeadwordKey, row.headword)
                .eq(Word::getPronunciationKey, row.pronKey)
                .last("LIMIT 1"));
        if (exact != null) return exact;

        // 兼容旧库：旧 word 行没有 headword_key/pronunciation_key，只在词头大小写完全一致时复用。
        List<Word> candidates = wordMapper.selectList(Wrappers.<Word>lambdaQuery()
                .eq(Word::getNormalized, row.normalized));
        for (Word candidate : candidates) {
            boolean emptyHeadword = !WordTextUtils.hasText(candidate.getHeadwordKey());
            if (emptyHeadword && row.headword.equals(candidate.getWord())) {
                candidate.setHeadwordKey(row.headword);
                candidate.setPronunciationKey(row.pronKey);
                if (!WordTextUtils.hasText(candidate.getPhoneticUk())) candidate.setPhoneticUk(row.phoneticUk);
                if (!WordTextUtils.hasText(candidate.getPhoneticUs())) candidate.setPhoneticUs(row.phoneticUs);
                wordMapper.updateById(candidate);
                return candidate;
            }
        }

        Word word = new Word();
        word.setWord(row.word);
        word.setNormalized(row.normalized);
        word.setHeadwordKey(row.headword);
        word.setPronunciationKey(row.pronKey);
        word.setPhoneticUk(row.phoneticUk);
        word.setPhoneticUs(row.phoneticUs);
        word.setPos("");
        word.setMeaning("");
        word.setExampleEn(row.exampleEn);
        word.setExampleCn(row.exampleCn);
        wordMapper.insert(word);
        return word;
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
        addError(result, "第 " + (row.getRowNum() == null ? "?" : row.getRowNum()) + " 行：" + message);
    }

    private void addError(ImportResultVo result, String message) {
        if (result.getErrors().size() >= 100) return;
        List<String> errors = new ArrayList<>(result.getErrors());
        errors.add(message);
        result.setErrors(errors);
    }

    private static final class PreparedRow {
        private final Integer seq;
        private final String word;
        private final String headword;
        private final String normalized;
        private final String pronKey;
        private final String phoneticUk;
        private final String phoneticUs;
        private final String exampleEn;
        private final String exampleCn;
        private final Map<String, WordTextUtils.ParsedSense> senses = new LinkedHashMap<>();

        private PreparedRow(WordImportVo row, String headword, String normalized, String pronKey) {
            this.seq = row.getSeq();
            this.word = row.getWord().trim();
            this.headword = headword;
            this.normalized = normalized;
            this.pronKey = pronKey;
            this.phoneticUk = row.getPhoneticUk();
            this.phoneticUs = row.getPhoneticUs();
            this.exampleEn = row.getExampleEn();
            this.exampleCn = row.getExampleCn();
        }

        private void addSenses(List<WordTextUtils.ParsedSense> values) {
            for (WordTextUtils.ParsedSense value : values) {
                senses.putIfAbsent(value.posKey() + "\u0000" + value.meaningKey(), value);
            }
        }
    }
}

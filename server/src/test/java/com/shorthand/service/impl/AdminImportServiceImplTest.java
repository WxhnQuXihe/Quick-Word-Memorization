package com.shorthand.service.impl;

import com.alibaba.excel.EasyExcel;
import com.shorthand.domain.ImportLog;
import com.shorthand.domain.Sense;
import com.shorthand.domain.Word;
import com.shorthand.domain.WordEntry;
import com.shorthand.domain.Wordbook;
import com.shorthand.domain.WordbookWord;
import com.shorthand.domain.vo.ImportResultVo;
import com.shorthand.domain.vo.WordImportVo;
import com.shorthand.mapper.ImportLogMapper;
import com.shorthand.mapper.SenseMapper;
import com.shorthand.mapper.WordEntryMapper;
import com.shorthand.mapper.WordEntrySenseMapper;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookMapper;
import com.shorthand.mapper.WordbookWordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminImportServiceImplTest {
    @Mock private WordbookMapper wordbookMapper;
    @Mock private WordMapper wordMapper;
    @Mock private WordbookWordMapper wordbookWordMapper;
    @Mock private WordEntryMapper wordEntryMapper;
    @Mock private SenseMapper senseMapper;
    @Mock private WordEntrySenseMapper wordEntrySenseMapper;
    @Mock private ImportLogMapper importLogMapper;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private AdminImportServiceImpl service;

    @Test
    void importsSevenColumnWorkbookAndCreatesSensesAndEntries() throws Exception {
        Wordbook book = new Wordbook();
        book.setId(1L);
        when(wordbookMapper.selectById(1L)).thenReturn(book);
        when(importLogMapper.insert(any(ImportLog.class))).thenAnswer(inv -> {
            inv.<ImportLog>getArgument(0).setId(99L);
            return 1;
        });
        when(wordMapper.selectList(any())).thenReturn(List.of());
        when(wordMapper.insert(any(Word.class))).thenAnswer(inv -> {
            inv.<Word>getArgument(0).setId(10L);
            return 1;
        });
        when(senseMapper.selectOne(any())).thenReturn(null);
        AtomicLong senseId = new AtomicLong(20);
        when(senseMapper.insert(any(Sense.class))).thenAnswer(inv -> {
            inv.<Sense>getArgument(0).setId(senseId.getAndIncrement());
            return 1;
        });
        when(wordEntryMapper.selectOne(any())).thenReturn(null);
        when(wordEntryMapper.insert(any(WordEntry.class))).thenAnswer(inv -> {
            inv.<WordEntry>getArgument(0).setId(30L);
            return 1;
        });
        when(wordEntrySenseMapper.selectOne(any())).thenReturn(null);
        when(wordbookWordMapper.selectList(any())).thenReturn(List.of());
        when(wordbookWordMapper.selectOne(any())).thenReturn(null);
        when(wordbookWordMapper.insert(any(WordbookWord.class))).thenReturn(1);
        when(wordbookWordMapper.selectCount(any())).thenReturn(2L);
        doAnswer(inv -> {
            inv.<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>getArgument(0)
                    .accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        WordImportVo first = row(1, "abandon", "v. 放弃\nn. 放任；狂热");
        WordImportVo second = row(2, "a bit (of)", "adv. 一点");
        MockMultipartFile file = workbook(first, second);

        ImportResultVo result = service.importExcel(file, 1L, 1L);

        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getSuccessRows());
        assertEquals(0, result.getFailRows());
        verify(wordMapper, times(2)).insert(any(Word.class));
        verify(senseMapper, times(3)).insert(any(Sense.class));
        verify(wordEntryMapper, times(2)).insert(any(WordEntry.class));
        verify(wordbookWordMapper, times(2)).insert(any(WordbookWord.class));
    }

    @Test
    void rejectsWholeWorkbookWhenAnyMeaningLineHasNoPos() throws Exception {
        Wordbook book = new Wordbook();
        book.setId(1L);
        when(wordbookMapper.selectById(1L)).thenReturn(book);
        when(importLogMapper.insert(any(ImportLog.class))).thenAnswer(inv -> {
            inv.<ImportLog>getArgument(0).setId(99L);
            return 1;
        });

        ImportResultVo result = service.importExcel(
                workbook(row(1, "abandon", "没有词性的释义")), 1L, 1L);

        assertEquals(1, result.getFailRows());
        assertEquals(0, result.getSuccessRows());
        verify(wordMapper, never()).insert(any(Word.class));
        verify(senseMapper, never()).insert(any(Sense.class));
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    private static WordImportVo row(int seq, String word, String meaning) {
        WordImportVo row = new WordImportVo();
        row.setSeq(seq);
        row.setWord(word);
        row.setPhoneticUk("/test/");
        row.setPhoneticUs("/test/");
        row.setMeaning(meaning);
        return row;
    }

    private static MockMultipartFile workbook(WordImportVo... rows) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, WordImportVo.class).sheet("词书").doWrite(List.of(rows));
        return new MockMultipartFile("file", "words.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }
}

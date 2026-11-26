package com.shorthand.service.impl;

import com.alibaba.excel.EasyExcel;
import com.shorthand.domain.Word;
import com.shorthand.domain.Wordbook;
import com.shorthand.domain.WordbookWord;
import com.shorthand.domain.vo.WordImportVo;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookMapper;
import com.shorthand.mapper.WordbookWordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWordbookServiceImplTest {
    @Mock
    private WordbookMapper wordbookMapper;
    @Mock
    private WordbookWordMapper wordbookWordMapper;
    @Mock
    private WordMapper wordMapper;

    @InjectMocks
    private AdminWordbookServiceImpl service;

    @Test
    void exportExcelKeepsBookOrderAndBothPhonetics() {
        Wordbook book = new Wordbook();
        book.setId(1L);

        WordbookWord first = new WordbookWord();
        first.setWordbookId(1L);
        first.setWordId(10L);
        first.setSeq(2);
        WordbookWord second = new WordbookWord();
        second.setWordbookId(1L);
        second.setWordId(20L);
        second.setSeq(5);

        Word abandon = new Word();
        abandon.setId(10L);
        abandon.setWord("abandon");
        abandon.setPhoneticUk("/əˈbændən/");
        abandon.setPhoneticUs("/əˈbændən/");
        abandon.setPos("v.");
        abandon.setMeaning("v. 放弃");
        Word phrase = new Word();
        phrase.setId(20L);
        phrase.setWord("a bit (of)");
        phrase.setPhoneticUs("/ə bɪt/");
        phrase.setMeaning("一点");

        when(wordbookMapper.selectById(1L)).thenReturn(book);
        when(wordbookWordMapper.selectList(any())).thenReturn(List.of(first, second));
        when(wordMapper.selectBatchIds(anyList())).thenReturn(List.of(abandon, phrase));

        byte[] content = service.exportExcel(1L);
        List<WordImportVo> rows = EasyExcel.read(new ByteArrayInputStream(content))
                .head(WordImportVo.class).sheet().doReadSync();

        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).getSeq());
        assertEquals("abandon", rows.get(0).getWord());
        assertEquals("/əˈbændən/", rows.get(0).getPhoneticUk());
        assertEquals("/əˈbændən/", rows.get(0).getPhoneticUs());
        assertEquals(5, rows.get(1).getSeq());
        assertEquals("a bit (of)", rows.get(1).getWord());
        assertEquals("/ə bɪt/", rows.get(1).getPhoneticUs());
    }
}

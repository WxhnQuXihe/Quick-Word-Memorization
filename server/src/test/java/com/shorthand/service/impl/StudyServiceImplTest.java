package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.shorthand.domain.StudyGroup;
import com.shorthand.domain.StudyGroupWord;
import com.shorthand.domain.UserWord;
import com.shorthand.domain.UserWordbook;
import com.shorthand.domain.Word;
import com.shorthand.domain.WordAudio;
import com.shorthand.domain.WordbookWord;
import com.shorthand.domain.vo.WordVo;
import com.shorthand.mapper.StudyGroupMapper;
import com.shorthand.mapper.StudyGroupWordMapper;
import com.shorthand.mapper.UserWordMapper;
import com.shorthand.mapper.UserWordbookMapper;
import com.shorthand.mapper.WordAudioMapper;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookWordMapper;
import com.shorthand.service.StudyLockManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyServiceImplTest {

    @Mock
    private UserWordbookMapper userWordbookMapper;
    @Mock
    private WordbookWordMapper wordbookWordMapper;
    @Mock
    private WordMapper wordMapper;
    @Mock
    private WordAudioMapper wordAudioMapper;
    @Mock
    private StudyGroupMapper studyGroupMapper;
    @Mock
    private StudyGroupWordMapper studyGroupWordMapper;
    @Mock
    private UserWordMapper userWordMapper;
    @Mock
    private StudyLockManager studyLockManager;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private StudyServiceImpl studyService;

    @BeforeEach
    void setUpLocksAndTransactions() {
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get())
                .when(studyLockManager).withLock(anyLong(), anyLong(), any());
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
    }

    @BeforeAll
    static void initMyBatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, StudyGroup.class);
        TableInfoHelper.initTableInfo(assistant, StudyGroupWord.class);
        TableInfoHelper.initTableInfo(assistant, UserWord.class);
        TableInfoHelper.initTableInfo(assistant, UserWordbook.class);
        TableInfoHelper.initTableInfo(assistant, Word.class);
        TableInfoHelper.initTableInfo(assistant, WordAudio.class);
        TableInfoHelper.initTableInfo(assistant, WordbookWord.class);
    }

    @Test
    void filterCandidates_excludesKnownAndUsedWords() {
        List<Long> candidates = StudyServiceImpl.filterCandidates(
                List.of(1L, 2L, 3L, 4L, 5L),
                Set.of(1L, 5L),
                Set.of(2L));

        assertThat(candidates).containsExactly(3L, 4L);
    }

    @Test
    void markKnown_doesNotReuseWordFromEarlierGroupWhenNoUnusedCandidateLeft() {
        long userId = 7L;
        long groupId = 100L;
        long wordId = 1L;
        long wordbookId = 1L;

        StudyGroupWord groupWord = new StudyGroupWord();
        groupWord.setId(1000L);
        groupWord.setGroupId(groupId);
        groupWord.setWordId(wordId);
        groupWord.setPosition(1);
        groupWord.setStatus(1);

        StudyGroup group = new StudyGroup();
        group.setId(groupId);
        group.setUserId(userId);
        group.setWordbookId(wordbookId);
        group.setRoundNo(1);
        group.setGroupNo(1);
        group.setStatus(0);

        UserWord state = new UserWord();
        state.setId(50L);
        state.setUserId(userId);
        state.setWordId(wordId);
        state.setIsKnown(0);
        state.setIsFavorite(0);

        when(studyGroupWordMapper.selectList(any())).thenReturn(List.of(groupWord));
        when(studyGroupMapper.selectById(groupId)).thenReturn(group);
        when(userWordMapper.selectList(any())).thenReturn(List.of(state));
        doReturn(List.of(1L)).when(userWordMapper).selectObjs(any());
        doReturn(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L))
                .when(wordbookWordMapper).selectObjs(any());
        when(studyGroupMapper.selectList(any())).thenReturn(List.of(group));
        doAnswer(invocation -> {
            Wrapper<?> wrapper = invocation.getArgument(0);
            // 模拟：本轮前 10 个词在当前组，第 11 个词在前面组已经出现。
            return wrapper.getSqlSegment().contains("IN")
                    ? List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L)
                    : List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        }).when(studyGroupWordMapper).selectObjs(any());
        when(userWordMapper.updateById(any(UserWord.class))).thenReturn(1);
        when(studyGroupWordMapper.updateById(any(StudyGroupWord.class))).thenReturn(1);

        WordVo result = studyService.markKnown(userId, groupId, wordId);

        assertThat(result).isNull();
        ArgumentCaptor<StudyGroupWord> captor = ArgumentCaptor.forClass(StudyGroupWord.class);
        verify(studyGroupWordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getWordId()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(2);
        verifyNoInteractions(wordMapper, wordAudioMapper);
    }

    @Test
    void markKnown_retriesWhenReplacementConflictsWithAnotherGroup() {
        long userId = 7L;
        long groupId = 100L;
        long wordId = 1L;
        long wordbookId = 1L;

        StudyGroupWord groupWord = new StudyGroupWord();
        groupWord.setId(1000L);
        groupWord.setGroupId(groupId);
        groupWord.setWordId(wordId);
        groupWord.setPosition(1);
        groupWord.setStatus(1);

        StudyGroup group = new StudyGroup();
        group.setId(groupId);
        group.setUserId(userId);
        group.setWordbookId(wordbookId);
        group.setRoundNo(1);
        group.setGroupNo(1);
        group.setStatus(0);

        UserWord state = new UserWord();
        state.setId(50L);
        state.setUserId(userId);
        state.setWordId(wordId);
        state.setSeenCount(1);
        state.setIsKnown(0);
        state.setIsFavorite(0);

        Word replacement = new Word();
        replacement.setId(2L);
        replacement.setWord("two");
        replacement.setMeaning("num. 二");

        when(studyGroupMapper.selectById(groupId)).thenReturn(group);
        when(studyGroupWordMapper.selectList(any())).thenReturn(List.of(groupWord));
        when(userWordMapper.selectList(any())).thenAnswer(invocation -> {
            Wrapper<?> wrapper = invocation.getArgument(0);
            return wrapper.getSqlSegment().contains("IN") ? List.of() : List.of(state);
        });
        doReturn(List.of()).when(userWordMapper).selectObjs(any());
        doReturn(List.of(1L, 2L)).when(wordbookWordMapper).selectObjs(any());
        when(studyGroupMapper.selectList(any())).thenReturn(List.of(group));
        doReturn(List.of(1L)).when(studyGroupWordMapper).selectObjs(any());
        when(userWordMapper.updateById(any(UserWord.class))).thenReturn(1);
        when(studyGroupWordMapper.updateById(any(StudyGroupWord.class)))
                .thenThrow(new DuplicateKeyException("uk_group_word"))
                .thenReturn(1);
        when(wordMapper.selectById(2L)).thenReturn(replacement);
        when(wordAudioMapper.selectList(any())).thenReturn(List.of());

        WordVo result = studyService.markKnown(userId, groupId, wordId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getWord()).isEqualTo("two");
        verify(studyGroupWordMapper, times(2)).updateById(any(StudyGroupWord.class));
    }
}

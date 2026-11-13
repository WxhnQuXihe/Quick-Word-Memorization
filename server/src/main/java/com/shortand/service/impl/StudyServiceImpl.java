package com.shortand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortand.common.BizException;
import com.shortand.common.PageResult;
import com.shortand.domain.StudyGroup;
import com.shortand.domain.StudyGroupWord;
import com.shortand.domain.UserWord;
import com.shortand.domain.UserWordbook;
import com.shortand.domain.Word;
import com.shortand.domain.WordbookWord;
import com.shortand.domain.vo.GroupVo;
import com.shortand.domain.vo.SenseVo;
import com.shortand.domain.vo.WordVo;
import com.shortand.mapper.StudyGroupMapper;
import com.shortand.mapper.StudyGroupWordMapper;
import com.shortand.mapper.UserWordMapper;
import com.shortand.mapper.UserWordbookMapper;
import com.shortand.mapper.WordMapper;
import com.shortand.mapper.WordbookWordMapper;
import com.shortand.service.IStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements IStudyService {
    private static final int GROUP_SIZE = 10;
    private static final Pattern POS_PATTERN = Pattern.compile(
            "^(v|n|adj|adv|prep|conj|pron|num|art|int|vt|vi|aux|abbr)\\.\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);

    private final UserWordbookMapper userWordbookMapper;
    private final WordbookWordMapper wordbookWordMapper;
    private final WordMapper wordMapper;
    private final StudyGroupMapper studyGroupMapper;
    private final StudyGroupWordMapper studyGroupWordMapper;
    private final UserWordMapper userWordMapper;

    @Override
    @Transactional
    public GroupVo current(Long userId, Long wordbookId) {
        UserWordbook userBook = getOrCreateUserBook(userId, wordbookId);
        return ensureGroup(userBook);
    }

    @Override
    @Transactional
    public GroupVo next(Long userId, Long groupId) {
        StudyGroup group = studyGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BizException("学习组不存在");
        }
        if (group.getStatus() == null || group.getStatus() == 0) {
            group.setStatus(1);
            group.setFinishTime(LocalDateTime.now());
            studyGroupMapper.updateById(group);
        }
        UserWordbook userBook = getOrCreateUserBook(userId, group.getWordbookId());
        return ensureGroup(userBook);
    }

    @Override
    @Transactional
    public WordVo markKnown(Long userId, Long groupId, Long wordId) {
        StudyGroupWord groupWord = studyGroupWordMapper.selectList(Wrappers.<StudyGroupWord>lambdaQuery()
                        .eq(StudyGroupWord::getGroupId, groupId)
                        .eq(StudyGroupWord::getWordId, wordId)
                        .eq(StudyGroupWord::getStatus, 1))
                .stream().findFirst().orElse(null);
        if (groupWord == null) {
            throw new BizException("这个词不在当前组里");
        }
        StudyGroup group = studyGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BizException("学习组不存在");
        }

        UserWord state = findUserWord(userId, wordId);
        if (state == null) {
            state = new UserWord();
            state.setUserId(userId);
            state.setWordId(wordId);
            state.setSeenCount(1);
            state.setFirstWordbookId(group.getWordbookId());
            state.setIsFavorite(0);
            state.setIsKnown(1);
            state.setKnownTime(LocalDateTime.now());
            userWordMapper.insert(state);
        } else {
            state.setIsKnown(1);
            state.setKnownTime(LocalDateTime.now());
            userWordMapper.updateById(state);
        }

        List<Long> bookWordIds = bookWordIds(group.getWordbookId());
        Set<Long> knownIds = knownIds(userId);
        Set<Long> currentGroupIds = currentGroupIds(groupId);
        List<Long> candidates = bookWordIds.stream()
                .filter(id -> !knownIds.contains(id) && !currentGroupIds.contains(id))
                .toList();

        if (candidates.isEmpty()) {
            groupWord.setStatus(2);
            groupWord.setWordId(null);
            groupWord.setUpdateTime(LocalDateTime.now());
            studyGroupWordMapper.updateById(groupWord);
            return null;
        }

        Long replacementId = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        groupWord.setWordId(replacementId);
        groupWord.setUpdateTime(LocalDateTime.now());
        studyGroupWordMapper.updateById(groupWord);
        touchSeen(userId, List.of(replacementId), group.getWordbookId());
        return toWordVo(wordMapper.selectById(replacementId));
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long userId, Long groupId, Long wordId) {
        StudyGroupWord groupWord = studyGroupWordMapper.selectList(Wrappers.<StudyGroupWord>lambdaQuery()
                        .eq(StudyGroupWord::getGroupId, groupId)
                        .eq(StudyGroupWord::getWordId, wordId)
                        .eq(StudyGroupWord::getStatus, 1))
                .stream().findFirst().orElse(null);
        if (groupWord == null) {
            throw new BizException("这个词不在当前组里");
        }
        UserWord state = findUserWord(userId, wordId);
        boolean next;
        if (state == null) {
            state = new UserWord();
            state.setUserId(userId);
            state.setWordId(wordId);
            state.setSeenCount(1);
            state.setIsFavorite(1);
            state.setFavoriteTime(LocalDateTime.now());
            state.setIsKnown(0);
            userWordMapper.insert(state);
            next = true;
        } else {
            next = state.getIsFavorite() == null || state.getIsFavorite() == 0;
            state.setIsFavorite(next ? 1 : 0);
            state.setFavoriteTime(next ? LocalDateTime.now() : null);
            userWordMapper.updateById(state);
        }
        return next;
    }

    @Override
    public PageResult favorites(Long userId, long pageNo, long pageSize) {
        Page<UserWord> page = userWordMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<UserWord>lambdaQuery()
                        .eq(UserWord::getUserId, userId)
                        .eq(UserWord::getIsFavorite, 1)
                        .orderByDesc(UserWord::getFavoriteTime));
        return PageResult.of(toWordVos(page.getRecords()), page.getTotal(), pageNo, pageSize);
    }

    @Override
    public PageResult known(Long userId, long pageNo, long pageSize) {
        Page<UserWord> page = userWordMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<UserWord>lambdaQuery()
                        .eq(UserWord::getUserId, userId)
                        .eq(UserWord::getIsKnown, 1)
                        .orderByDesc(UserWord::getKnownTime));
        return PageResult.of(toWordVos(page.getRecords()), page.getTotal(), pageNo, pageSize);
    }

    @Override
    public boolean removeFavorite(Long userId, Long wordId) {
        UserWord state = findUserWord(userId, wordId);
        if (state == null) return false;
        state.setIsFavorite(0);
        state.setFavoriteTime(null);
        userWordMapper.updateById(state);
        return true;
    }

    @Override
    public boolean removeKnown(Long userId, Long wordId) {
        UserWord state = findUserWord(userId, wordId);
        if (state == null) return false;
        state.setIsKnown(0);
        state.setKnownTime(null);
        userWordMapper.updateById(state);
        return true;
    }

    private GroupVo ensureGroup(UserWordbook userBook) {
        StudyGroup active = findActiveGroup(userBook);
        if (active != null) {
            return toGroupVo(active);
        }
        List<Long> candidates = candidates(userBook);
        if (candidates.isEmpty()) {
            userBook.setCurrentRoundNo(userBook.getCurrentRoundNo() + 1);
            userBook.setLastStudyTime(LocalDateTime.now());
            userWordbookMapper.updateById(userBook);
            candidates = candidates(userBook);
        }
        if (candidates.isEmpty()) {
            GroupVo empty = new GroupVo();
            empty.setFinished(true);
            empty.setRoundNo(userBook.getCurrentRoundNo());
            empty.setWords(List.of());
            return empty;
        }

        int groupNo = nextGroupNo(userBook);
        List<Long> selected = candidates.subList(0, Math.min(GROUP_SIZE, candidates.size()));
        StudyGroup group = new StudyGroup();
        group.setUserId(userBook.getUserId());
        group.setWordbookId(userBook.getWordbookId());
        group.setRoundNo(userBook.getCurrentRoundNo());
        group.setGroupNo(groupNo);
        group.setWordCount(selected.size());
        group.setStatus(0);
        studyGroupMapper.insert(group);

        for (int i = 0; i < selected.size(); i++) {
            StudyGroupWord row = new StudyGroupWord();
            row.setGroupId(group.getId());
            row.setWordId(selected.get(i));
            row.setPosition(i + 1);
            row.setStatus(1);
            studyGroupWordMapper.insert(row);
        }
        touchSeen(userBook.getUserId(), selected, userBook.getWordbookId());
        userBook.setLastStudyTime(LocalDateTime.now());
        userWordbookMapper.updateById(userBook);
        return toGroupVo(group);
    }

    private List<Long> candidates(UserWordbook userBook) {
        List<Long> bookWordIds = bookWordIds(userBook.getWordbookId());
        Set<Long> known = knownIds(userBook.getUserId());
        Set<Long> used = usedIds(userBook);
        return bookWordIds.stream()
                .filter(id -> !known.contains(id) && !used.contains(id))
                .toList();
    }

    private List<Long> bookWordIds(Long wordbookId) {
        return wordbookWordMapper.selectList(Wrappers.<WordbookWord>lambdaQuery()
                        .eq(WordbookWord::getWordbookId, wordbookId)
                        .orderByAsc(WordbookWord::getSeq))
                .stream().map(WordbookWord::getWordId).toList();
    }

    private Set<Long> knownIds(Long userId) {
        return userWordMapper.selectList(Wrappers.<UserWord>lambdaQuery()
                        .eq(UserWord::getUserId, userId)
                        .eq(UserWord::getIsKnown, 1))
                .stream().map(UserWord::getWordId).collect(Collectors.toSet());
    }

    private Set<Long> usedIds(UserWordbook userBook) {
        List<StudyGroup> groups = studyGroupMapper.selectList(Wrappers.<StudyGroup>lambdaQuery()
                .eq(StudyGroup::getUserId, userBook.getUserId())
                .eq(StudyGroup::getWordbookId, userBook.getWordbookId())
                .eq(StudyGroup::getRoundNo, userBook.getCurrentRoundNo()));
        if (groups.isEmpty()) return Set.of();
        List<Long> groupIds = groups.stream().map(StudyGroup::getId).toList();
        return studyGroupWordMapper.selectList(Wrappers.<StudyGroupWord>lambdaQuery()
                        .in(StudyGroupWord::getGroupId, groupIds))
                .stream().map(StudyGroupWord::getWordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> currentGroupIds(Long groupId) {
        return studyGroupWordMapper.selectList(Wrappers.<StudyGroupWord>lambdaQuery()
                        .eq(StudyGroupWord::getGroupId, groupId))
                .stream().map(StudyGroupWord::getWordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private StudyGroup findActiveGroup(UserWordbook userBook) {
        return studyGroupMapper.selectList(Wrappers.<StudyGroup>lambdaQuery()
                        .eq(StudyGroup::getUserId, userBook.getUserId())
                        .eq(StudyGroup::getWordbookId, userBook.getWordbookId())
                        .eq(StudyGroup::getRoundNo, userBook.getCurrentRoundNo())
                        .eq(StudyGroup::getStatus, 0)
                        .orderByDesc(StudyGroup::getId))
                .stream().findFirst().orElse(null);
    }

    private int nextGroupNo(UserWordbook userBook) {
        List<StudyGroup> groups = studyGroupMapper.selectList(Wrappers.<StudyGroup>lambdaQuery()
                .eq(StudyGroup::getUserId, userBook.getUserId())
                .eq(StudyGroup::getWordbookId, userBook.getWordbookId())
                .eq(StudyGroup::getRoundNo, userBook.getCurrentRoundNo()));
        return groups.stream().map(StudyGroup::getGroupNo).max(Comparator.naturalOrder()).orElse(0) + 1;
    }

    private UserWordbook getOrCreateUserBook(Long userId, Long wordbookId) {
        UserWordbook userBook = userWordbookMapper.selectList(Wrappers.<UserWordbook>lambdaQuery()
                        .eq(UserWordbook::getUserId, userId)
                        .eq(UserWordbook::getWordbookId, wordbookId))
                .stream().findFirst().orElse(null);
        if (userBook == null) {
            userBook = new UserWordbook();
            userBook.setUserId(userId);
            userBook.setWordbookId(wordbookId);
            userBook.setCurrentRoundNo(1);
            userBook.setLastStudyTime(LocalDateTime.now());
            userWordbookMapper.insert(userBook);
        }
        return userBook;
    }

    private UserWord findUserWord(Long userId, Long wordId) {
        return userWordMapper.selectList(Wrappers.<UserWord>lambdaQuery()
                        .eq(UserWord::getUserId, userId)
                        .eq(UserWord::getWordId, wordId))
                .stream().findFirst().orElse(null);
    }

    private void touchSeen(Long userId, List<Long> wordIds, Long wordbookId) {
        if (wordIds.isEmpty()) return;
        Map<Long, UserWord> existing = userWordMapper.selectList(Wrappers.<UserWord>lambdaQuery()
                        .eq(UserWord::getUserId, userId)
                        .in(UserWord::getWordId, wordIds))
                .stream().collect(Collectors.toMap(UserWord::getWordId, Function.identity()));
        LocalDateTime now = LocalDateTime.now();
        for (Long wordId : wordIds) {
            UserWord state = existing.get(wordId);
            if (state == null) {
                state = new UserWord();
                state.setUserId(userId);
                state.setWordId(wordId);
                state.setSeenCount(1);
                state.setLastSeenTime(now);
                state.setFirstWordbookId(wordbookId);
                state.setIsFavorite(0);
                state.setIsKnown(0);
                userWordMapper.insert(state);
            } else {
                state.setSeenCount((state.getSeenCount() == null ? 0 : state.getSeenCount()) + 1);
                state.setLastSeenTime(now);
                if (state.getFirstWordbookId() == null) state.setFirstWordbookId(wordbookId);
                userWordMapper.updateById(state);
            }
        }
    }

    private GroupVo toGroupVo(StudyGroup group) {
        List<StudyGroupWord> rows = studyGroupWordMapper.selectList(Wrappers.<StudyGroupWord>lambdaQuery()
                        .eq(StudyGroupWord::getGroupId, group.getId())
                        .orderByAsc(StudyGroupWord::getPosition));
        List<Long> ids = rows.stream().map(StudyGroupWord::getWordId)
                .filter(Objects::nonNull).toList();
        Map<Long, Word> words = ids.isEmpty() ? Map.of() : wordMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity()));
        List<WordVo> vos = new ArrayList<>();
        for (StudyGroupWord row : rows) {
            if (row.getWordId() != null && words.containsKey(row.getWordId())) {
                vos.add(toWordVo(words.get(row.getWordId())));
            }
        }
        GroupVo vo = new GroupVo();
        vo.setGroupId(group.getId());
        vo.setRoundNo(group.getRoundNo());
        vo.setGroupNo(group.getGroupNo());
        vo.setWordCount(group.getWordCount());
        vo.setFinished(false);
        vo.setWords(vos);
        return vo;
    }

    private List<WordVo> toWordVos(List<UserWord> rows) {
        if (rows.isEmpty()) return List.of();
        List<Long> ids = rows.stream().map(UserWord::getWordId).toList();
        Map<Long, Word> words = wordMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity()));
        return rows.stream().map(row -> toWordVo(words.get(row.getWordId())))
                .filter(Objects::nonNull).toList();
    }

    private WordVo toWordVo(Word word) {
        if (word == null) return null;
        WordVo vo = new WordVo();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setPhoneticUk(word.getPhoneticUk());
        vo.setPhoneticUs(word.getPhoneticUs());
        List<SenseVo> senses = new ArrayList<>();
        String meaning = word.getMeaning() == null ? "" : word.getMeaning();
        for (String line : meaning.split("\\R")) {
            String text = line.trim();
            if (text.isEmpty()) continue;
            Matcher m = POS_PATTERN.matcher(text);
            SenseVo sense = new SenseVo();
            if (m.matches()) {
                sense.setPos(m.group(1).toLowerCase() + ".");
                sense.setMeaning(m.group(2));
            } else {
                sense.setPos(word.getPos() == null ? "" : word.getPos());
                sense.setMeaning(text);
            }
            senses.add(sense);
        }
        if (senses.isEmpty()) {
            SenseVo sense = new SenseVo();
            sense.setPos(word.getPos() == null ? "" : word.getPos());
            sense.setMeaning("");
            senses.add(sense);
        }
        vo.setSenses(senses);
        return vo;
    }
}

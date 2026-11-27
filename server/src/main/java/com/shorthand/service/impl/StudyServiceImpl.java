package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorthand.common.BizException;
import com.shorthand.common.PageResult;
import com.shorthand.domain.Sense;
import com.shorthand.domain.StudyGroup;
import com.shorthand.domain.StudyGroupWord;
import com.shorthand.domain.UserWord;
import com.shorthand.domain.UserWordbook;
import com.shorthand.domain.Word;
import com.shorthand.domain.WordAudio;
import com.shorthand.domain.WordEntry;
import com.shorthand.domain.WordEntrySense;
import com.shorthand.domain.WordbookWord;
import com.shorthand.domain.vo.GroupVo;
import com.shorthand.domain.vo.SenseVo;
import com.shorthand.domain.vo.WordVo;
import com.shorthand.mapper.SenseMapper;
import com.shorthand.mapper.StudyGroupMapper;
import com.shorthand.mapper.StudyGroupWordMapper;
import com.shorthand.mapper.UserWordMapper;
import com.shorthand.mapper.UserWordbookMapper;
import com.shorthand.mapper.WordEntryMapper;
import com.shorthand.mapper.WordEntrySenseMapper;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookWordMapper;
import com.shorthand.mapper.WordAudioMapper;
import com.shorthand.service.IStudyService;
import com.shorthand.service.StudyLockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final WordEntryMapper wordEntryMapper;
    private final SenseMapper senseMapper;
    private final WordEntrySenseMapper wordEntrySenseMapper;
    private final WordAudioMapper wordAudioMapper;
    private final StudyGroupMapper studyGroupMapper;
    private final StudyGroupWordMapper studyGroupWordMapper;
    private final UserWordMapper userWordMapper;
    private final StudyLockManager studyLockManager;
    private final TransactionTemplate transactionTemplate;

    @Override
    public GroupVo current(Long userId, Long wordbookId) {
        return studyLockManager.withLock(userId, wordbookId,
                () -> transactionTemplate.execute(status -> doCurrent(userId, wordbookId)));
    }

    private GroupVo doCurrent(Long userId, Long wordbookId) {
        UserWordbook userBook = getOrCreateUserBook(userId, wordbookId);
        return ensureGroup(userBook);
    }

    @Override
    public GroupVo next(Long userId, Long groupId) {
        StudyGroup snapshot = requireGroup(userId, groupId);
        return studyLockManager.withLock(userId, snapshot.getWordbookId(),
                () -> transactionTemplate.execute(status -> doNext(userId, groupId)));
    }

    private GroupVo doNext(Long userId, Long groupId) {
        StudyGroup group = requireGroup(userId, groupId);
        if (group.getStatus() == null || group.getStatus() == 0) {
            group.setStatus(1);
            group.setFinishTime(LocalDateTime.now());
            studyGroupMapper.updateById(group);
        }
        UserWordbook userBook = getOrCreateUserBook(userId, group.getWordbookId());
        return ensureGroup(userBook);
    }

    @Override
    public WordVo markKnown(Long userId, Long groupId, Long wordId) {
        StudyGroup snapshot = requireGroup(userId, groupId);
        return studyLockManager.withLock(userId, snapshot.getWordbookId(),
                () -> transactionTemplate.execute(status -> doMarkKnown(userId, groupId, wordId)));
    }

    private WordVo doMarkKnown(Long userId, Long groupId, Long wordId) {
        StudyGroup group = requireGroup(userId, groupId);
        StudyGroupWord groupWord = studyGroupWordMapper.selectList(Wrappers.<StudyGroupWord>lambdaQuery()
                        .eq(StudyGroupWord::getGroupId, groupId)
                        .eq(StudyGroupWord::getWordId, wordId)
                        .eq(StudyGroupWord::getStatus, 1))
                .stream().findFirst().orElse(null);
        if (groupWord == null) {
            throw new BizException("这个词不在当前组里");
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

        // 组内 word_id 唯一约束可能在多实例并发时冲突；冲突后重新取候选并重试。
        for (int attempt = 0; attempt < 3; attempt++) {
            List<Long> candidates = candidates(userId, group.getWordbookId(), group.getRoundNo());
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
            try {
                studyGroupWordMapper.updateById(groupWord);
                touchSeen(userId, List.of(replacementId), group.getWordbookId());
                return toWordVo(wordMapper.selectById(replacementId),
                        entryIdForBook(group.getWordbookId(), replacementId),
                        audioUrls(List.of(replacementId)).get(replacementId));
            } catch (DuplicateKeyException e) {
                if (attempt == 2) {
                    throw new BizException("换词冲突，请重试");
                }
            }
        }
        throw new BizException("换词冲突，请重试");
    }

    private StudyGroup requireGroup(Long userId, Long groupId) {
        StudyGroup group = studyGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BizException("学习组不存在");
        }
        return group;
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
        List<Long> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool, ThreadLocalRandom.current());
        List<Long> selected = pool.subList(0, Math.min(GROUP_SIZE, pool.size()));
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
        return candidates(userBook.getUserId(), userBook.getWordbookId(), userBook.getCurrentRoundNo());
    }

    private List<Long> candidates(Long userId, Long wordbookId, Integer roundNo) {
        return filterCandidates(bookWordIds(wordbookId), knownIds(userId),
                usedIds(userId, wordbookId, roundNo));
    }

    static List<Long> filterCandidates(List<Long> bookWordIds, Set<Long> knownIds, Set<Long> usedIds) {
        return bookWordIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !knownIds.contains(id) && !usedIds.contains(id))
                .toList();
    }

    private List<Long> bookWordIds(Long wordbookId) {
        return wordbookWordMapper.selectObjs(Wrappers.<WordbookWord>lambdaQuery()
                        .select(WordbookWord::getWordId)
                        .eq(WordbookWord::getWordbookId, wordbookId)
                        .orderByAsc(WordbookWord::getSeq))
                .stream().map(v -> ((Number) v).longValue()).distinct().toList();
    }

    private Set<Long> knownIds(Long userId) {
        return userWordMapper.selectObjs(Wrappers.<UserWord>lambdaQuery()
                        .select(UserWord::getWordId)
                        .eq(UserWord::getUserId, userId)
                        .eq(UserWord::getIsKnown, 1))
                .stream().map(v -> ((Number) v).longValue()).collect(Collectors.toSet());
    }

    private Set<Long> usedIds(Long userId, Long wordbookId, Integer roundNo) {
        List<StudyGroup> groups = studyGroupMapper.selectList(Wrappers.<StudyGroup>lambdaQuery()
                .eq(StudyGroup::getUserId, userId)
                .eq(StudyGroup::getWordbookId, wordbookId)
                .eq(StudyGroup::getRoundNo, roundNo));
        if (groups.isEmpty()) return Set.of();
        List<Long> groupIds = groups.stream().map(StudyGroup::getId).toList();
        return studyGroupWordMapper.selectObjs(Wrappers.<StudyGroupWord>lambdaQuery()
                        .select(StudyGroupWord::getWordId)
                        .in(StudyGroupWord::getGroupId, groupIds))
                .stream().filter(Objects::nonNull).map(v -> ((Number) v).longValue())
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
        Map<Long, Long> entries = entryIdsForBook(group.getWordbookId(), ids);
        Map<Long, String> audio = audioUrls(ids);
        List<WordVo> vos = new ArrayList<>();
        for (StudyGroupWord row : rows) {
            if (row.getWordId() != null && words.containsKey(row.getWordId())) {
                vos.add(toWordVo(words.get(row.getWordId()), entries.get(row.getWordId()),
                        audio.get(row.getWordId())));
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
        Map<Long, String> audio = audioUrls(ids);
        return rows.stream().map(row -> toWordVo(words.get(row.getWordId()), null,
                        audio.get(row.getWordId())))
                .filter(Objects::nonNull).toList();
    }

    private Map<Long, Long> entryIdsForBook(Long wordbookId, List<Long> wordIds) {
        if (wordIds.isEmpty()) return Map.of();
        return wordbookWordMapper.selectList(Wrappers.<WordbookWord>lambdaQuery()
                        .eq(WordbookWord::getWordbookId, wordbookId)
                        .in(WordbookWord::getWordId, wordIds))
                .stream().filter(r -> r.getEntryId() != null)
                .collect(Collectors.toMap(WordbookWord::getWordId, WordbookWord::getEntryId, (a, b) -> a));
    }

    private Long entryIdForBook(Long wordbookId, Long wordId) {
        WordbookWord relation = wordbookWordMapper.selectOne(Wrappers.<WordbookWord>lambdaQuery()
                .eq(WordbookWord::getWordbookId, wordbookId)
                .eq(WordbookWord::getWordId, wordId)
                .last("LIMIT 1"));
        return relation == null ? null : relation.getEntryId();
    }

    private Map<Long, String> audioUrls(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return wordAudioMapper.selectList(Wrappers.<WordAudio>lambdaQuery()
                        .in(WordAudio::getWordId, ids)
                        .eq(WordAudio::getStatus, 2)
                        .eq(WordAudio::getAccent, "us")
                        .eq(WordAudio::getVoice, "af_heart"))
                .stream().collect(Collectors.toMap(WordAudio::getWordId,
                        a -> "/audio/" + a.getObjectKey(), (a, b) -> a));
    }

    private WordVo toWordVo(Word word) {
        return toWordVo(word, null, null);
    }

    private WordVo toWordVo(Word word, Long entryId, String audioUrl) {
        if (word == null) return null;
        WordVo vo = new WordVo();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setPhoneticUk(word.getPhoneticUk());
        vo.setPhoneticUs(word.getPhoneticUs());

        List<SenseVo> senses = entryId == null ? sensesForLexeme(word.getId()) : sensesForEntry(entryId);
        if (senses.isEmpty()) senses = legacySenses(word);
        vo.setSenses(senses);
        vo.setAudioUrl(audioUrl);
        return vo;
    }

    private List<SenseVo> sensesForEntry(Long entryId) {
        List<WordEntrySense> links = wordEntrySenseMapper.selectList(Wrappers.<WordEntrySense>lambdaQuery()
                .eq(WordEntrySense::getEntryId, entryId)
                .orderByAsc(WordEntrySense::getSort));
        if (links.isEmpty()) return List.of();
        List<Long> senseIds = links.stream().map(WordEntrySense::getSenseId).toList();
        Map<Long, Sense> senses = senseMapper.selectBatchIds(senseIds).stream()
                .collect(Collectors.toMap(Sense::getId, Function.identity()));
        List<SenseVo> result = new ArrayList<>();
        for (WordEntrySense link : links) {
            Sense sense = senses.get(link.getSenseId());
            if (sense != null) result.add(toSenseVo(sense, link.getDisplayMeaning()));
        }
        return result;
    }

    private List<SenseVo> sensesForLexeme(Long wordId) {
        return senseMapper.selectList(Wrappers.<Sense>lambdaQuery()
                        .eq(Sense::getWordId, wordId)
                        .orderByAsc(Sense::getId))
                .stream().map(s -> toSenseVo(s, null)).toList();
    }

    private SenseVo toSenseVo(Sense sense, String displayMeaning) {
        SenseVo vo = new SenseVo();
        vo.setPos(sense.getPos());
        String meaning = displayMeaning;
        if (meaning == null || meaning.trim().isEmpty()) meaning = sense.getMeaning();
        vo.setMeaning(meaning);
        return vo;
    }

    private List<SenseVo> legacySenses(Word word) {
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
        return senses;
    }
}

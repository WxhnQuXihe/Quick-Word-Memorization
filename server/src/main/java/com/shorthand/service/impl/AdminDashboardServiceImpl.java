package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shorthand.domain.AppUser;
import com.shorthand.domain.Feedback;
import com.shorthand.domain.ImportLog;
import com.shorthand.domain.StudyGroup;
import com.shorthand.domain.UserWord;
import com.shorthand.domain.Wordbook;
import com.shorthand.domain.vo.AdminDashboardVo;
import com.shorthand.domain.vo.UserVo;
import com.shorthand.domain.vo.WordbookVo;
import com.shorthand.mapper.AppUserMapper;
import com.shorthand.mapper.FeedbackMapper;
import com.shorthand.mapper.ImportLogMapper;
import com.shorthand.mapper.StudyGroupMapper;
import com.shorthand.mapper.UserWordMapper;
import com.shorthand.mapper.WordMapper;
import com.shorthand.mapper.WordbookMapper;
import com.shorthand.service.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements IAdminDashboardService {
    private final WordbookMapper wordbookMapper;
    private final WordMapper wordMapper;
    private final AppUserMapper appUserMapper;
    private final UserWordMapper userWordMapper;
    private final FeedbackMapper feedbackMapper;
    private final StudyGroupMapper studyGroupMapper;
    private final ImportLogMapper importLogMapper;

    @Override
    public AdminDashboardVo overview() {
        AdminDashboardVo vo = new AdminDashboardVo();
        vo.setBookCount(wordbookMapper.selectCount(null));
        vo.setWordCount(wordMapper.selectCount(null));
        vo.setUserCount(appUserMapper.selectCount(null));
        vo.setFavoriteCount(userWordMapper.selectCount(Wrappers.<UserWord>lambdaQuery()
                .eq(UserWord::getIsFavorite, 1)));
        vo.setKnownCount(userWordMapper.selectCount(Wrappers.<UserWord>lambdaQuery()
                .eq(UserWord::getIsKnown, 1)));
        vo.setOpenFeedbackCount(feedbackMapper.selectCount(Wrappers.<Feedback>lambdaQuery()
                .eq(Feedback::getStatus, 0)));
        vo.setBooks(wordbookMapper.selectList(Wrappers.<Wordbook>lambdaQuery()
                        .orderByAsc(Wordbook::getSort).orderByDesc(Wordbook::getId))
                .stream().map(this::bookVo).toList());
        vo.setRecentUsers(appUserMapper.selectList(Wrappers.<AppUser>lambdaQuery()
                        .orderByDesc(AppUser::getId)).stream().limit(5).map(this::userVo).toList());
        vo.setRecentImports(importLogMapper.selectList(Wrappers.<ImportLog>lambdaQuery()
                .orderByDesc(ImportLog::getId)).stream().limit(5).toList());
        vo.setActivity(activity());
        Runtime rt = Runtime.getRuntime();
        vo.setJvmUsedMb((rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
        vo.setJvmMaxMb(rt.maxMemory() / 1024 / 1024);
        vo.setUptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        return vo;
    }

    private List<Long> activity() {
        LocalDate start = LocalDate.now().minusDays(13);
        List<StudyGroup> groups = studyGroupMapper.selectList(Wrappers.<StudyGroup>lambdaQuery()
                .eq(StudyGroup::getStatus, 1)
                .isNotNull(StudyGroup::getFinishTime)
                .ge(StudyGroup::getFinishTime, start.atStartOfDay()));
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate day = start.plusDays(i);
            result.add(groups.stream()
                    .filter(g -> g.getFinishTime() != null && g.getFinishTime().toLocalDate().equals(day))
                    .count());
        }
        return result;
    }

    private WordbookVo bookVo(Wordbook book) {
        WordbookVo vo = new WordbookVo();
        vo.setId(book.getId());
        vo.setCode(book.getCode());
        vo.setName(book.getName());
        vo.setWordCount(book.getWordCount());
        vo.setStatus(book.getStatus());
        vo.setSort(book.getSort());
        vo.setDifficulty(book.getDifficulty());
        vo.setUpdateTime(book.getUpdateTime());
        return vo;
    }

    private UserVo userVo(AppUser user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }
}

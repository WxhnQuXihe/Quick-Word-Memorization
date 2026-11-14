package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorthand.common.BizException;
import com.shorthand.common.PageResult;
import com.shorthand.domain.Announcement;
import com.shorthand.domain.bo.AnnouncementBo;
import com.shorthand.mapper.AnnouncementMapper;
import com.shorthand.service.IAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements IAnnouncementService {
    private final AnnouncementMapper announcementMapper;

    @Override
    public List<Announcement> published() {
        return announcementMapper.selectList(Wrappers.<Announcement>lambdaQuery()
                        .eq(Announcement::getStatus, 1)
                        .orderByDesc(Announcement::getPublishTime)
                        .orderByDesc(Announcement::getId))
                .stream().limit(5).toList();
    }

    @Override
    public PageResult list(long pageNo, long pageSize) {
        Page<Announcement> page = announcementMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<Announcement>lambdaQuery().orderByDesc(Announcement::getId));
        return PageResult.of(page.getRecords(), page.getTotal(), pageNo, pageSize);
    }

    @Override
    public Announcement save(AnnouncementBo bo) {
        Announcement a = new Announcement();
        apply(a, bo);
        if (a.getStatus() == 1) a.setPublishTime(LocalDateTime.now());
        announcementMapper.insert(a);
        return a;
    }

    @Override
    public Announcement update(Long id, AnnouncementBo bo) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) throw new BizException("公告不存在");
        apply(a, bo);
        if (bo.getStatus() != null && bo.getStatus() == 1 && a.getPublishTime() == null) {
            a.setPublishTime(LocalDateTime.now());
        }
        announcementMapper.updateById(a);
        return a;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) throw new BizException("公告不存在");
        a.setStatus(status == null ? 0 : status);
        if (status != null && status == 1 && a.getPublishTime() == null) a.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(a);
    }

    @Override
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }

    private void apply(Announcement a, AnnouncementBo bo) {
        a.setTitle(bo.getTitle());
        a.setContent(bo.getContent());
        if (bo.getStatus() != null) a.setStatus(bo.getStatus());
        if (a.getStatus() == null) a.setStatus(0);
    }
}

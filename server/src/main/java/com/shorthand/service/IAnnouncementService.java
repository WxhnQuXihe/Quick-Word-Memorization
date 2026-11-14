package com.shorthand.service;

import com.shorthand.common.PageResult;
import com.shorthand.domain.Announcement;
import com.shorthand.domain.bo.AnnouncementBo;

import java.util.List;

public interface IAnnouncementService {
    List<Announcement> published();

    PageResult list(long pageNo, long pageSize);

    Announcement save(AnnouncementBo bo);

    Announcement update(Long id, AnnouncementBo bo);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}

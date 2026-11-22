package com.shorthand.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 单实例内按「用户 + 词书」串行化学习写操作。
 *
 * <p>锁对象不主动清理：用户/词书数量有限，避免锁对象被替换时出现两个线程同时持锁的竞态。
 * 多实例部署时应改为数据库行锁或分布式锁。</p>
 */
@Component
public class StudyLockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(Long userId, Long wordbookId, Supplier<T> action) {
        String key = userId + ":" + wordbookId;
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}

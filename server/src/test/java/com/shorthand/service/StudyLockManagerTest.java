package com.shorthand.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class StudyLockManagerTest {

    @Test
    void serializesSameUserAndWordbook() throws Exception {
        StudyLockManager manager = new StudyLockManager();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();

        try {
            Future<?> first = pool.submit(() -> manager.withLock(1L, 1L, () -> {
                inside.incrementAndGet();
                firstEntered.countDown();
                await(releaseFirst);
                inside.decrementAndGet();
                return null;
            }));

            assertThat(firstEntered.await(2, TimeUnit.SECONDS)).isTrue();

            Future<?> second = pool.submit(() -> {
                secondStarted.countDown();
                return manager.withLock(1L, 1L, () -> {
                    int current = inside.incrementAndGet();
                    maxInside.accumulateAndGet(current, Math::max);
                    inside.decrementAndGet();
                    return null;
                });
            });

            assertThat(secondStarted.await(2, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(150);
            assertThat(maxInside.get()).isZero();

            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);

            assertThat(maxInside.get()).isEqualTo(1);
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

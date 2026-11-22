package com.onpu.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {

    /** 時間を進められる時計 */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-22T00:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        limiter = new LoginAttemptLimiter(clock);
    }

    private void failTimes(String loginId, int times) {
        for (int i = 0; i < times; i++) {
            limiter.checkAllowed(loginId);
            limiter.recordFailure(loginId);
        }
    }

    @Test
    void 失敗が上限未満ならログインできる() {
        failTimes("alice", LoginAttemptLimiter.MAX_FAILURES - 1);
        assertDoesNotThrow(() -> limiter.checkAllowed("alice"));
    }

    @Test
    void 上限まで失敗するとロックされ_待つ時間を返す() {
        failTimes("alice", LoginAttemptLimiter.MAX_FAILURES);
        clock.advance(Duration.ofMinutes(5));

        TooManyLoginAttemptsException e =
            assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("alice"));
        assertEquals(Duration.ofMinutes(10).toSeconds(), e.getRetryAfterSeconds());
        assertTrue(e.getMessage().contains("10分"));
    }

    @Test
    void 最初の失敗から期間が過ぎるとロックが解ける() {
        failTimes("alice", LoginAttemptLimiter.MAX_FAILURES);
        clock.advance(LoginAttemptLimiter.WINDOW);
        assertDoesNotThrow(() -> limiter.checkAllowed("alice"));

        // 解けたあとは、また1回目から数え直す
        failTimes("alice", LoginAttemptLimiter.MAX_FAILURES - 1);
        assertDoesNotThrow(() -> limiter.checkAllowed("alice"));
    }

    @Test
    void 成功すると回数がリセットされる() {
        failTimes("alice", LoginAttemptLimiter.MAX_FAILURES - 1);
        limiter.recordSuccess("alice");
        failTimes("alice", LoginAttemptLimiter.MAX_FAILURES - 1);
        assertDoesNotThrow(() -> limiter.checkAllowed("alice"));
    }

    @Test
    void ほかのログインIDには影響しない() {
        failTimes("alice", LoginAttemptLimiter.MAX_FAILURES);
        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("alice"));
        assertDoesNotThrow(() -> limiter.checkAllowed("bob"));
    }
}

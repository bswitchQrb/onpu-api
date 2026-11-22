package com.onpu.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ログインの失敗回数をログイン ID ごとに数えて、総当たり攻撃を防ぐ。
 *
 * <p>同じログイン ID で {@link #WINDOW} 以内に {@link #MAX_FAILURES} 回失敗すると、
 * 最初の失敗から {@link #WINDOW} が経つまでログインを受け付けない。成功したら回数をリセットする。
 *
 * <p>IP アドレスでは数えない。Render のプロキシの後ろでは全員が同じ IP に見えるため、
 * 無関係な利用者までまとめて止めてしまう。
 *
 * <p>回数はメモリに持つので、アプリを再起動するとリセットされる（インスタンスは1台の前提）。
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    /** この件数を超えたら、期限切れの記録を掃除する（メモリが増え続けないように） */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Failures(int count, Instant windowStart) {}

    private final Clock clock;
    private final ConcurrentHashMap<String, Failures> failures = new ConcurrentHashMap<>();

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    /** テスト用に時計を差し替えられるようにする */
    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /** ロック中なら {@link TooManyLoginAttemptsException} を投げる */
    public void checkAllowed(String loginId) {
        Failures f = failures.get(loginId);
        if (f == null || isExpired(f) || f.count() < MAX_FAILURES) {
            return;
        }
        Duration remaining = Duration.between(clock.instant(), f.windowStart().plus(WINDOW));
        throw new TooManyLoginAttemptsException(Math.max(1, remaining.toSeconds()));
    }

    public void recordFailure(String loginId) {
        Instant now = clock.instant();
        failures.compute(loginId, (id, f) ->
            f == null || isExpired(f) ? new Failures(1, now) : new Failures(f.count() + 1, f.windowStart()));
        if (failures.size() > CLEANUP_THRESHOLD) {
            failures.values().removeIf(this::isExpired);
        }
    }

    public void recordSuccess(String loginId) {
        failures.remove(loginId);
    }

    private boolean isExpired(Failures f) {
        return !clock.instant().isBefore(f.windowStart().plus(WINDOW));
    }
}

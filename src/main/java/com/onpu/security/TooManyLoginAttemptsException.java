package com.onpu.security;

/** ログインの失敗が続いて、一時的にログインを止めているときの例外（HTTP 429 で返す） */
public class TooManyLoginAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super("ログインの失敗が続いたため、しばらくログインできません。%d分ほど待ってからもう一度お試しください"
            .formatted(Math.max(1, (retryAfterSeconds + 59) / 60)));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

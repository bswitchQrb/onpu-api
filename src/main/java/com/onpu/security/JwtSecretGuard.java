package com.onpu.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * ローカル開発用の JWT の鍵（application-local.yml）が、local プロファイル以外で使われていたら起動を止める。
 * その鍵は公開リポジトリに載っているので、本番で使うと誰でもトークンを偽造できてしまう。
 */
@Component
class JwtSecretGuard {

    /** application-local.yml の鍵に含まれる目印 */
    static final String LOCAL_SECRET_MARKER = "only-for-localhost";

    JwtSecretGuard(JwtProperties props, Environment env) {
        if (props.getSecret().contains(LOCAL_SECRET_MARKER) && !env.matchesProfiles("local")) {
            throw new IllegalStateException(
                "ローカル開発用の JWT の鍵が local プロファイル以外で使われています。環境変数 JWT_SECRET を設定してください");
        }
    }
}

package com.onpu.service;

import com.onpu.dto.*;
import com.onpu.jooq.tables.records.UsersRecord;
import com.onpu.security.JwtService;
import com.onpu.security.LoginAttemptLimiter;
import org.jooq.DSLContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.onpu.jooq.Tables.*;

@Service
public class AuthService {

    private final DSLContext dsl;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public AuthService(DSLContext dsl, PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginAttemptLimiter loginAttemptLimiter) {
        this.dsl = dsl;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean exists = dsl.fetchExists(
            dsl.selectFrom(USERS).where(USERS.LOGIN_ID.eq(request.loginId()))
        );
        if (exists) {
            throw new IllegalArgumentException("このIDは既に使われています");
        }

        UsersRecord user = dsl.newRecord(USERS);
        user.setLoginId(request.loginId());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.store();

        String token = jwtService.generateToken(user.getId(), user.getLoginId());
        return new AuthResponse(token, user.getNickname());
    }

    public AuthResponse login(LoginRequest request) {
        // 失敗が続いている ID は、パスワードを確かめる前に断る（総当たり対策）
        loginAttemptLimiter.checkAllowed(request.loginId());

        UsersRecord user = dsl.selectFrom(USERS)
            .where(USERS.LOGIN_ID.eq(request.loginId()))
            .fetchOptional()
            .orElse(null);

        // 存在しない ID も同じように数えて、ID が存在するかどうかを推測されないようにする
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptLimiter.recordFailure(request.loginId());
            throw new IllegalArgumentException("IDまたはパスワードが正しくありません");
        }
        loginAttemptLimiter.recordSuccess(request.loginId());

        String token = jwtService.generateToken(user.getId(), user.getLoginId());
        return new AuthResponse(token, user.getNickname());
    }

    public UserResponse getUser(Long userId) {
        UsersRecord user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOptional()
            .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));

        return new UserResponse(user.getId(), user.getLoginId(), user.getNickname());
    }
}

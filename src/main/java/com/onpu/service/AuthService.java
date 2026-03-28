package com.onpu.service;

import com.onpu.dto.*;
import com.onpu.jooq.tables.records.UsersRecord;
import com.onpu.security.JwtService;
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

    public AuthService(DSLContext dsl, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.dsl = dsl;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        UsersRecord user = dsl.selectFrom(USERS)
            .where(USERS.LOGIN_ID.eq(request.loginId()))
            .fetchOptional()
            .orElseThrow(() -> new IllegalArgumentException("IDまたはパスワードが正しくありません"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("IDまたはパスワードが正しくありません");
        }

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

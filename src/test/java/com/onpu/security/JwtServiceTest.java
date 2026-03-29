package com.onpu.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-unit-tests-at-least-256-bits-long-enough!!");
        props.setExpirationMs(86400000L);
        jwtService = new JwtService(props);
    }

    @Test
    void generateToken_returnsNonEmptyString() {
        String token = jwtService.generateToken(1L, "testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getUserIdFromToken_returnsCorrectUserId() {
        String token = jwtService.generateToken(42L, "testuser");
        Long userId = jwtService.getUserIdFromToken(token);
        assertEquals(42L, userId);
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtService.generateToken(1L, "testuser");
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_returnsFalseForInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_returnsFalseForEmptyToken() {
        assertFalse(jwtService.validateToken(""));
    }
}

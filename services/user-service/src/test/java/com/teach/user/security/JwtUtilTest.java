package com.teach.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "TeachingPlatformSecretKeyMustBe32BytesLong!";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600_000L); // 1小时有效
    }

    @Test
    @DisplayName("UNIT-TC-JWT-01: 成功生成 JWT 并解析 claims 内容")
    void testCreateAndParseToken_Success() {
        String token = jwtUtil.createToken(88L, "student88", "student");

        assertNotNull(token);
        Claims claims = jwtUtil.parse(token);

        assertEquals("88", claims.getSubject());
        assertEquals("student88", claims.get("username", String.class));
        assertEquals("student", claims.get("role", String.class));
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("UNIT-TC-JWT-02: 解析篡改的 Token 抛出异常")
    void testParseTamperedToken_ThrowsException() {
        String token = jwtUtil.createToken(88L, "student88", "student");
        String tamperedToken = token + "tampered";

        assertThrows(JwtException.class, () -> jwtUtil.parse(tamperedToken));
    }
}
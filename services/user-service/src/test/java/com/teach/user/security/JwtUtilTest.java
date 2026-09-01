package com.teach.user.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtUtilTest {

    private static final String SECRET = "ChangeMe_ThisIs_A_32BytePlus_SecretKey_ForHS256_XYZ";

    @Test
    void createAndParseShouldRoundTripClaims() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 86_400_000L);

        String token = jwtUtil.createToken(42L, "alice", "student");

        Claims claims = jwtUtil.parse(token);
        assertEquals("42", claims.getSubject());
        assertEquals("alice", claims.get("username", String.class));
        assertEquals("student", claims.get("role", String.class));
        assertNotNull(claims.getExpiration());
    }
}

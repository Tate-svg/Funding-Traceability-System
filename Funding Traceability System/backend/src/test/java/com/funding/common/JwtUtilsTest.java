package com.funding.common;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JWT 工具单元测试：签发、解析、角色提取、过期与篡改
 */
class JwtUtilsTest {

    private static final String SECRET = "funding-traceability-secret-key-2024-very-long";

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = new JwtUtils();
        jwtUtils.setSecret(SECRET);
        jwtUtils.setExpire(86400000L);
    }

    @Test
    @DisplayName("签发后可解析出 userId/username/role")
    void generateAndParseShouldCarryClaims() {
        String token = JwtUtils.generate(1L, "audit_admin", "AUDIT_ADMIN");
        Claims claims = JwtUtils.parse(token);
        assertNotNull(claims);
        assertEquals(1L, JwtUtils.extractUserId(claims));
        assertEquals("audit_admin", JwtUtils.extractUsername(claims));
        assertEquals("AUDIT_ADMIN", JwtUtils.extractRole(claims));
    }

    @Test
    @DisplayName("篡改 token 后解析失败")
    void parseShouldRejectTamperedToken() {
        String token = JwtUtils.generate(2L, "sponsor", "SPONSOR");
        String tampered = token.substring(0, token.length() - 2) + "xy";
        assertThrows(Exception.class, () -> JwtUtils.parse(tampered));
    }

    @Test
    @DisplayName("过期 token 解析失败，防止旧凭证长期有效")
    void parseShouldRejectExpiredToken() {
        JwtUtils jwtUtils = new JwtUtils();
        jwtUtils.setSecret(SECRET);
        jwtUtils.setExpire(-1000L);
        String expired = JwtUtils.generate(3L, "disburse_admin", "DISBURSE_ADMIN");
        assertThrows(Exception.class, () -> JwtUtils.parse(expired));
    }

    @Test
    @DisplayName("缺失 role 时安全返回 null")
    void extractRoleShouldReturnNullWhenAbsent() {
        Claims claims = JwtUtils.parse(JwtUtils.generate(9L, "x", null));
        assertEquals(9L, JwtUtils.extractUserId(claims));
        assertNull(JwtUtils.extractRole(claims));
    }
}

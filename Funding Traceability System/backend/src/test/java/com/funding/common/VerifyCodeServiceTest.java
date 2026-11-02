package com.funding.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 溯源核验码单元测试：签名生成、验签、防篡改
 */
class VerifyCodeServiceTest {

    private VerifyCodeService service;

    @BeforeEach
    void setUp() {
        service = new VerifyCodeService("funding-verify-secret-2024");
    }

    @Test
    @DisplayName("生成的核验码可正确还原单据ID，且同一单据恒定")
    void encodeAndDecodeShouldRoundTrip() {
        String code1 = service.encode(1L);
        String code2 = service.encode(1L);
        assertEquals(code1, code2);
        assertEquals(1L, service.decode(code1));
    }

    @Test
    @DisplayName("篡改签名后验签失败")
    void decodeShouldRejectTamperedSignature() {
        String code = service.encode(6L);
        String tampered = code.substring(0, code.length() - 1)
                + (code.endsWith("A") ? "B" : "A");
        assertNull(service.decode(tampered));
    }

    @Test
    @DisplayName("篡改单据ID后验签失败，防止枚举遍历")
    void decodeShouldRejectTamperedFundId() {
        String code = service.encode(6L);
        String forged = "999" + code.substring(code.indexOf('.'));
        assertNull(service.decode(forged));
    }

    @Test
    @DisplayName("非法格式核验码返回 null 而不抛异常")
    void decodeShouldRejectMalformedCode() {
        assertNull(service.decode(null));
        assertNull(service.decode("abc"));
        assertNull(service.decode(".abc"));
        assertNull(service.decode("1."));
        assertNull(service.decode("abc.def"));
    }

    @Test
    @DisplayName("不同单据生成的核验码不同")
    void differentFundsShouldHaveDifferentCodes() {
        assertNotEquals(service.encode(1L), service.encode(2L));
    }

    @Test
    @DisplayName("fundId 为空时拒绝生成")
    void encodeShouldRejectNullFundId() {
        assertThrows(IllegalArgumentException.class, () -> service.encode(null));
    }
}

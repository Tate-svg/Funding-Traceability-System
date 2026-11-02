package com.funding.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 敏感字段二次遮挡工具单元测试（纯逻辑，无数据库依赖）
 */
class SensitiveMaskUtilTest {

    private final SensitiveMaskUtil util = new SensitiveMaskUtil();

    @Test
    @DisplayName("手机号二次遮挡：保留前3后2")
    void maskPhoneShouldKeepPrefixAndSuffix() {
        assertEquals("138****02", util.maskPhone("13812340002", true));
    }

    @Test
    @DisplayName("关闭二次遮挡时手机号原样返回")
    void maskPhoneShouldReturnOriginalWhenDisabled() {
        assertEquals("13812340002", util.maskPhone("13812340002", false));
        assertNull(util.maskPhone(null, true));
    }

    @Test
    @DisplayName("过短手机号统一遮挡为 ****")
    void maskPhoneShouldHandleShortValue() {
        assertEquals("****", util.maskPhone("123456", true));
        assertEquals("****", util.maskPhone("123", true));
    }

    @Test
    @DisplayName("家庭住址二次遮挡：保留前3个字符")
    void maskAddressShouldKeepProvinceAndCity() {
        assertEquals("北京市 ****", util.maskAddress("北京市海淀区中关村大街1号", true));
    }

    @Test
    @DisplayName("超短家庭住址不会越界")
    void maskAddressShouldHandleShortValue() {
        assertEquals("北***", util.maskAddress("北京", true));
        assertEquals("京***", util.maskAddress("京", true));
    }

    @Test
    @DisplayName("关闭二次遮挡时住址原样返回")
    void maskAddressShouldReturnOriginalWhenDisabled() {
        assertEquals("北京市海淀区", util.maskAddress("北京市海淀区", false));
        assertNull(util.maskAddress(null, true));
    }

    @Test
    @DisplayName("银行卡二次遮挡：保留前4后4")
    void maskBankCardShouldKeepPrefixAndSuffix() {
        assertEquals("6222********1234", util.maskBankCard("6222020202021234", true));
        assertEquals("6222", util.maskBankCard("6222", true));
        assertEquals("6222020202021234", util.maskBankCard("6222020202021234", false));
    }
}

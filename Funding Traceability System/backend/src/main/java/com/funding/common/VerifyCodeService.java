package com.funding.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 资助凭证核验码服务
 *
 * 核验码 = 单据ID + HMAC-SHA256 签名片段，无需额外建表：
 * - 签名保证核验码不可伪造、不可枚举（无法通过改数字遍历他人单据）
 * - 同一单据的核验码恒定，单据状态更新后扫码仍返回最新数据（实时联动）
 * - 仅暴露单据 ID 与签名，二维码本身不携带任何业务/隐私信息
 */
@Component
public class VerifyCodeService {

    private static final String ALGORITHM = "HmacSHA256";

    /** 签名片段长度：16 位 Base64URL（约 96 bit），兼顾防枚举与二维码密度 */
    private static final int SIGN_LENGTH = 16;

    private final byte[] secret;

    public VerifyCodeService(@Value("${verify.secret:funding-verify-secret-2024}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成核验码：{fundId}.{sign}
     */
    public String encode(Long fundId) {
        if (fundId == null) {
            throw new IllegalArgumentException("fundId 不能为空");
        }
        String payload = String.valueOf(fundId);
        return payload + "." + sign(payload);
    }

    /**
     * 解析核验码：签名校验通过返回单据 ID，否则返回 null
     */
    public Long decode(String code) {
        if (code == null) {
            return null;
        }
        String value = code.trim();
        int idx = value.lastIndexOf('.');
        if (idx <= 0 || idx >= value.length() - 1) {
            return null;
        }
        String payload = value.substring(0, idx);
        String sign = value.substring(idx + 1);
        if (!MessageDigest.isEqual(sign.getBytes(StandardCharsets.UTF_8),
                sign(payload).getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        try {
            return Long.valueOf(payload);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return encoded.length() > SIGN_LENGTH ? encoded.substring(0, SIGN_LENGTH) : encoded;
        } catch (Exception e) {
            throw new IllegalStateException("核验码签名失败", e);
        }
    }
}

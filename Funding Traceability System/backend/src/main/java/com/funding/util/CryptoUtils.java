package com.funding.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 学生隐私字段 AES 加解密工具
 * 密钥配置于 funding.privacy.aes-key（必须16位），密文 Base64 输出
 */
@Component
public class CryptoUtils {

    private static String key;

    @Value("${funding.privacy.aes-key}")
    private String aesKey;

    @PostConstruct
    public void init() {
        if (aesKey == null || aesKey.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalStateException("funding.privacy.aes-key 必须为16位字符");
        }
        key = aesKey;
    }

    /** 加密：明文 -> Base64 密文；空值原样返回 */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("隐私字段加密失败", e);
        }
    }

    /** 解密：Base64 密文 -> 明文；空值原样返回 */
    public static String decrypt(String enc) {
        if (enc == null || enc.isEmpty()) {
            return enc;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(enc)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("隐私字段解密失败", e);
        }
    }
}

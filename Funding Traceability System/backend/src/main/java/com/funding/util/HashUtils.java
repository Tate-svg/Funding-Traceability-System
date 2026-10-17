package com.funding.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 哈希工具：资助凭证关键字段 SHA-256 存证
 */
public class HashUtils {

    private HashUtils() {
    }

    /**
     * 计算字符串 SHA-256，输出小写 hex
     */
    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }
}

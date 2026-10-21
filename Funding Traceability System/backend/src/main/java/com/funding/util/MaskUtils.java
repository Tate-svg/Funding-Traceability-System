package com.funding.util;

import java.util.regex.Pattern;

/**
 * 脱敏工具：回访材料自动脱敏，只保留核验必要信息。
 * 规则：身份证保留前3后2、手机号保留前3后2、银行卡保留前4后4、邮箱保留首字符与域名。
 * 处理顺序保证长号码优先（身份证 > 银行卡 > 手机号），避免短号正则误匹配长号片段。
 */
public class MaskUtils {

    /** 18位身份证：保留前3后2（中间13位） */
    private static final Pattern ID_CARD_18 = Pattern.compile("(\\d{3})\\d{13}([\\dXx]{2})");
    /** 15位身份证：保留前3后3 */
    private static final Pattern ID_CARD_15 = Pattern.compile("(\\d{3})\\d{9}(\\d{3})");
    /** 银行卡 16~19 位连续数字：保留前4后4 */
    private static final Pattern BANK_CARD = Pattern.compile("(\\d{4})\\d{8,11}(\\d{4})");
    /** 手机号：保留前3后2 */
    private static final Pattern PHONE = Pattern.compile("(1[3-9]\\d)\\d{6}(\\d{2})");
    /** 邮箱：保留首字符与域名 */
    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private MaskUtils() {
    }

    /**
     * 对文本中的敏感信息脱敏；null 安全。
     */
    public static String mask(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        String s = raw;
        s = ID_CARD_18.matcher(s).replaceAll("$1*************$2");
        s = ID_CARD_15.matcher(s).replaceAll("$1*********$2");
        s = BANK_CARD.matcher(s).replaceAll("$1********$2");
        s = PHONE.matcher(s).replaceAll("$1******$2");
        s = EMAIL.matcher(s).replaceAll("$1***$2");
        return s;
    }

    /**
     * 生成脱敏回访记录：将 回访内容/情况说明/存在问题 脱敏后合并。
     */
    public static String maskRecord(String content, String situation, String problem) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "回访内容", content);
        appendSection(sb, "情况说明", situation);
        appendSection(sb, "存在问题", problem);
        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" | ");
        }
        sb.append(label).append(": ").append(mask(value.trim()));
    }
}

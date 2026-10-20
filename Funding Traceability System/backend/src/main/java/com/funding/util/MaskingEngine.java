package com.funding.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动脱敏引擎
 * 识别身份证、手机号、银行卡、长串数字等敏感内容；
 * 用途一：自由文本提交前二次校验（未打码敏感内容拦截）；
 * 用途二：上传材料自动打码建议（前端预览态命中区间）
 */
public class MaskingEngine {

    private MaskingEngine() {
    }

    /** 命中片段 */
    @Data
    public static class Hit {
        private int start;
        private int end;
        private String rule;
        private String text;
        private String masked;
    }

    /** 规则表（顺序即优先级，先命中先占用区间，避免身份证被长数字规则重复命中） */
    private static final LinkedHashMap<String, Pattern> RULES = new LinkedHashMap<>();

    static {
        RULES.put("ID_CARD", Pattern.compile("\\d{17}[\\dXx]"));
        RULES.put("PHONE", Pattern.compile("1[3-9]\\d{9}"));
        RULES.put("BANK_CARD", Pattern.compile("\\d{16,19}"));
        RULES.put("LONG_DIGIT", Pattern.compile("\\d{8,}"));
    }

    /**
     * 扫描文本中的敏感片段（区间不重叠）
     */
    public static List<Hit> scan(String text) {
        List<Hit> hits = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return hits;
        }
        for (Map.Entry<String, Pattern> entry : RULES.entrySet()) {
            Matcher m = entry.getValue().matcher(text);
            while (m.find()) {
                if (covered(hits, m.start(), m.end())) {
                    continue;
                }
                Hit hit = new Hit();
                hit.setStart(m.start());
                hit.setEnd(m.end());
                hit.setRule(entry.getKey());
                hit.setText(m.group());
                hit.setMasked(maskOf(entry.getKey(), m.group()));
                hits.add(hit);
            }
        }
        hits.sort((a, b) -> a.getStart() - b.getStart());
        return hits;
    }

    /**
     * 自动打码：命中片段全部替换为掩码形式
     */
    public static String autoMask(String text) {
        List<Hit> hits = scan(text);
        if (hits.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        // 从后往前替换，保持偏移量有效
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            sb.replace(h.getStart(), h.getEnd(), h.getMasked());
        }
        return sb.toString();
    }

    /**
     * 打码规则：手机 138****5678 / 身份证前3后4 / 银行卡前4后4 / 长数字前2后2
     */
    public static String maskOf(String rule, String raw) {
        if (raw == null) {
            return null;
        }
        int len = raw.length();
        switch (rule) {
            case "PHONE":
                return raw.substring(0, 3) + "****" + raw.substring(len - 4);
            case "ID_CARD":
                return raw.substring(0, 3) + repeat('*', len - 7) + raw.substring(len - 4);
            case "BANK_CARD":
                return raw.substring(0, 4) + repeat('*', len - 8) + raw.substring(len - 4);
            default:
                return raw.substring(0, 2) + repeat('*', len - 4) + raw.substring(len - 2);
        }
    }

    private static boolean covered(List<Hit> hits, int start, int end) {
        for (Hit h : hits) {
            if (start < h.getEnd() && end > h.getStart()) {
                return true;
            }
        }
        return false;
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(Math.max(n, 0));
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}

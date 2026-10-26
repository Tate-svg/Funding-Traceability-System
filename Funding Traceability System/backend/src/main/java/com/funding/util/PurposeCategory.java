package com.funding.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资金用途分类
 *
 * 将自由填写的用途文本归入固定 7 类标准用途，用于统一统计口径（仪表盘「资金用途分布」按分类绘制）。
 * 分类规则为关键字匹配，按「越具体越靠前」的顺序命中，未命中任何关键字时归入「其他专项资助」。
 */
public final class PurposeCategory {

    /** 学费：学年学费（统一归类，不再拆分春秋学期） */
    public static final String TUITION = "学费";
    /** 住宿费：在校宿舍住宿费用 */
    public static final String ACCOMMODATION = "住宿费";
    /** 生活费：餐饮、日常基础生活开支 */
    public static final String LIVING = "生活费";
    /** 学习资料费：教材、书本、习题、线上学习资源 */
    public static final String MATERIAL = "学习资料费";
    /** 实验耗材费：实验课程材料、实训配件、专业工具 */
    public static final String LAB = "实验耗材费";
    /** 医保体检费：大学生医保、入学/年度体检 */
    public static final String MEDICAL = "医保体检费";
    /** 其他专项资助：临时困难补助、综合助学补贴、交通补助等 */
    public static final String OTHER = "其他专项资助";

    /** 关键字规则：顺序敏感，先命中先归类 */
    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    /** 展示顺序：与产品定义的用途分类顺序保持一致 */
    private static final List<String> ORDER = Collections.unmodifiableList(Arrays.asList(
            TUITION, ACCOMMODATION, LIVING, MATERIAL, LAB, MEDICAL, OTHER));

    static {
        RULES.put(MEDICAL, Arrays.asList("医保", "体检", "医疗", "保险"));
        RULES.put(ACCOMMODATION, Arrays.asList("住宿", "宿舍", "寝室"));
        RULES.put(LAB, Arrays.asList("实验", "实训", "耗材", "材料费", "配件", "器材", "工具"));
        RULES.put(MATERIAL, Arrays.asList("教材", "书本", "书籍", "资料", "习题", "文具", "学习资源", "网课"));
        RULES.put(TUITION, Arrays.asList("学费", "学杂费", "学期"));
        RULES.put(LIVING, Arrays.asList("生活费", "餐饮", "伙食", "生活开支", "日常"));
    }

    private PurposeCategory() {
    }

    /**
     * 分类结果：7 类固定顺序，便于统计时按稳定顺序输出
     */
    public static List<String> all() {
        return ORDER;
    }

    /**
     * 将用途文本归类到标准用途
     *
     * @param purpose 原始用途文本（可为空）
     * @return 7 类标准用途之一，永不返回 null
     */
    public static String classify(String purpose) {
        if (purpose == null || purpose.trim().isEmpty()) {
            return OTHER;
        }
        String text = purpose.trim();
        for (Map.Entry<String, List<String>> rule : RULES.entrySet()) {
            for (String keyword : rule.getValue()) {
                if (text.contains(keyword)) {
                    return rule.getKey();
                }
            }
        }
        return OTHER;
    }
}

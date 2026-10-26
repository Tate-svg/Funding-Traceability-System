package com.funding.util;

import com.funding.entity.TimestampRecord;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 审计溯源单据渲染器（Java2D）
 *
 * 仅用于审计专用导出：整页铺满半透明水印「当前操作者账号 + 导出时间」，
 * 水印以低透明度覆盖正文之上，既保留单据可读性，又为截图/转发留下归属痕迹。
 * 渲染结果为图片，PDF 导出时再嵌入图片，因此中文不依赖 PDF 内置字体。
 */
public final class AuditSheetRenderer {

    private static final int PAGE_W = 1240;
    private static final int PAGE_H = 1754;
    private static final int MARGIN = 50;
    private static final int FOOTER_TOP = PAGE_H - 62;
    private static final int ROW_START_Y = 226;
    private static final int ROW_MIN_H = 52;
    private static final int[] COL_W = {56, 152, 148, 84, 400, 120, 180};

    private static final Color C_TITLE = new Color(0x1E, 0x2A, 0x3A);
    private static final Color C_BODY = new Color(0x33, 0x41, 0x55);
    private static final Color C_SUB = new Color(0x64, 0x74, 0x8A);
    private static final Color C_MUTED = new Color(0x8A, 0x97, 0xA8);
    private static final Color C_PRIMARY = new Color(0x2B, 0x6C, 0xB0);
    private static final Color C_BORDER = new Color(0xE5, 0xEA, 0xF1);
    private static final Color C_HEAD_BG = new Color(0xF2, 0xF6, 0xFA);
    private static final Color C_DANGER = new Color(0xC2, 0x4A, 0x45);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FONT_FAMILY = resolveFontFamily();

    private static final Map<String, String> BIZ_TYPE_TEXT = new HashMap<>();

    static {
        BIZ_TYPE_TEXT.put("fund", "资助单");
        BIZ_TYPE_TEXT.put("student", "学生资料");
        BIZ_TYPE_TEXT.put("visit", "回访");
        BIZ_TYPE_TEXT.put("score", "评分");
        BIZ_TYPE_TEXT.put("score_item", "评分指标");
        BIZ_TYPE_TEXT.put("review_task", "复核任务");
        BIZ_TYPE_TEXT.put("sys_user", "用户");
        BIZ_TYPE_TEXT.put("system", "系统");
    }

    private AuditSheetRenderer() {
    }

    /**
     * 渲染审计溯源单据
     *
     * @param records    台账记录（同一筛选条件下的结果）
     * @param operator   当前操作者账号（水印/页眉）
     * @param exportTime 导出时间（水印/页眉）
     * @param filterDesc 筛选范围说明
     * @return 每个元素为一页图片
     */
    public static List<BufferedImage> render(List<TimestampRecord> records, String operator,
                                             LocalDateTime exportTime, String filterDesc) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D probeG = probe.createGraphics();
        probeG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        probeG.setFont(bodyFont());
        FontMetrics summaryFm = probeG.getFontMetrics(bodyFont());
        probeG.dispose();

        List<List<RowLayout>> pages = paginate(records, summaryFm);
        List<BufferedImage> images = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            images.add(renderPage(pages.get(i), i + 1, pages.size(), records.size(),
                    operator, exportTime, filterDesc));
        }
        return images;
    }

    private static List<List<RowLayout>> paginate(List<TimestampRecord> records, FontMetrics summaryFm) {
        List<List<RowLayout>> pages = new ArrayList<>();
        List<RowLayout> current = new ArrayList<>();
        int used = 0;
        for (TimestampRecord record : records) {
            RowLayout layout = layoutRow(record, summaryFm);
            if (!current.isEmpty() && ROW_START_Y + used + layout.height > FOOTER_TOP - 8) {
                pages.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(layout);
            used += layout.height;
        }
        if (current.isEmpty()) {
            current = new ArrayList<>();
        }
        pages.add(current);
        return pages;
    }

    private static RowLayout layoutRow(TimestampRecord record, FontMetrics summaryFm) {
        List<String> summaryLines = wrap(text(record.getSummary()), summaryFm, COL_W[4] - 18, 2);
        int height = Math.max(ROW_MIN_H, 24 + summaryLines.size() * 20);
        RowLayout layout = new RowLayout();
        layout.record = record;
        layout.summaryLines = summaryLines;
        layout.height = height;
        return layout;
    }

    private static BufferedImage renderPage(List<RowLayout> rows, int pageNo, int pageTotal, int recordTotal,
                                            String operator, LocalDateTime exportTime, String filterDesc) {
        BufferedImage image = new BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, PAGE_W, PAGE_H);

        drawHeader(g, operator, exportTime, filterDesc, recordTotal);
        drawTableHead(g);

        int y = ROW_START_Y;
        if (rows.isEmpty()) {
            g.setFont(bodyFont());
            g.setColor(C_MUTED);
            String empty = "当前筛选条件下暂无时间戳记录";
            int w = g.getFontMetrics().stringWidth(empty);
            g.drawString(empty, (PAGE_W - w) / 2, 420);
        }
        for (int i = 0; i < rows.size(); i++) {
            RowLayout layout = rows.get(i);
            drawRow(g, layout, y, i % 2 == 1);
            y += layout.height;
        }

        // 半透明水印：覆盖整页（含页眉与正文），低透明度不遮挡核心文字
        drawWatermark(g, operator, exportTime);

        g.setColor(C_BORDER);
        g.fillRect(MARGIN, FOOTER_TOP, PAGE_W - MARGIN * 2, 1);
        g.setFont(smallFont());
        g.setColor(C_MUTED);
        g.drawString("审计专用导出件 · 含操作人专属水印 · 仅限审计核验使用，禁止外传",
                MARGIN, FOOTER_TOP + 30);
        String pageText = "第 " + pageNo + " / " + pageTotal + " 页";
        int pw = g.getFontMetrics().stringWidth(pageText);
        g.drawString(pageText, PAGE_W - MARGIN - pw, FOOTER_TOP + 30);

        g.dispose();
        return image;
    }

    private static void drawHeader(Graphics2D g, String operator, LocalDateTime exportTime,
                                   String filterDesc, int recordTotal) {
        g.setColor(C_PRIMARY);
        g.fillRect(MARGIN, 48, 6, 40);

        g.setFont(titleFont());
        g.setColor(C_TITLE);
        g.drawString("资助资金审计溯源单据", MARGIN + 18, 78);

        g.setFont(smallFont());
        g.setColor(C_DANGER);
        String tag = "审计专用";
        int tagW = g.getFontMetrics().stringWidth(tag) + 22;
        g.fillRect(PAGE_W - MARGIN - tagW, 52, tagW, 26);
        g.setColor(Color.WHITE);
        g.drawString(tag, PAGE_W - MARGIN - tagW + 11, 71);

        g.setFont(bodyFont());
        g.setColor(C_SUB);
        g.drawString("导出账号：" + operator, MARGIN, 118);
        g.drawString("导出时间：" + TIME_FMT.format(exportTime), MARGIN + 330, 118);
        g.drawString("记录数：" + recordTotal + " 条", MARGIN + 690, 118);

        g.setFont(smallFont());
        g.setColor(C_MUTED);
        g.drawString("筛选范围：" + filterDesc, MARGIN, 150);

        g.setColor(C_BORDER);
        g.fillRect(MARGIN, 168, PAGE_W - MARGIN * 2, 1);
    }

    private static void drawTableHead(Graphics2D g) {
        g.setColor(C_HEAD_BG);
        g.fillRect(MARGIN, 180, PAGE_W - MARGIN * 2, 40);
        g.setFont(headFont());
        g.setColor(C_TITLE);
        String[] heads = {"序号", "时间", "业务单据", "节点", "摘要", "操作人", "存证哈希"};
        int x = MARGIN;
        for (int i = 0; i < heads.length; i++) {
            g.drawString(heads[i], x + 8, 206);
            x += COL_W[i];
        }
    }

    private static void drawRow(Graphics2D g, RowLayout layout, int top, boolean stripe) {
        int height = layout.height;
        if (stripe) {
            g.setColor(new Color(0xFA, 0xFC, 0xFE));
            g.fillRect(MARGIN, top, PAGE_W - MARGIN * 2, height);
        }
        g.setColor(C_BORDER);
        g.fillRect(MARGIN, top + height - 1, PAGE_W - MARGIN * 2, 1);

        TimestampRecord r = layout.record;
        g.setFont(bodyFont());
        FontMetrics fm = g.getFontMetrics();

        int x = MARGIN;
        // 序号
        g.setColor(C_PRIMARY);
        g.drawString("#" + r.getSerialNo(), x + 8, top + height / 2 + 6);
        x += COL_W[0];
        // 时间
        g.setColor(C_SUB);
        g.drawString(ellipsis(r.getTsTime() == null ? "—" : TIME_FMT.format(r.getTsTime()), fm, COL_W[1] - 16),
                x + 8, top + height / 2 + 6);
        x += COL_W[1];
        // 业务单据
        g.setColor(C_BODY);
        String biz = bizTypeText(r.getBizType()) + (r.getBizId() == null ? "" : " #" + r.getBizId());
        g.drawString(ellipsis(biz, fm, COL_W[2] - 16), x + 8, top + height / 2 + 6);
        x += COL_W[2];
        // 节点
        g.setColor(C_BODY);
        g.drawString(ellipsis(text(r.getNodeName()), fm, COL_W[3] - 16), x + 8, top + height / 2 + 6);
        x += COL_W[3];
        // 摘要（最多两行）
        g.setColor(C_BODY);
        int lineY = top + (height - layout.summaryLines.size() * 20) / 2 + 15;
        for (String line : layout.summaryLines) {
            g.drawString(line, x + 8, lineY);
            lineY += 20;
        }
        x += COL_W[4];
        // 操作人
        g.setColor(C_BODY);
        g.drawString(ellipsis(text(r.getOperatorName()), fm, COL_W[5] - 16), x + 8, top + height / 2 + 6);
        x += COL_W[5];
        // 哈希
        g.setFont(hashFont());
        g.setColor(C_MUTED);
        g.drawString(ellipsis(shortHash(r.getTsHash()), g.getFontMetrics(), COL_W[6] - 16),
                x + 8, top + height / 2 + 6);
    }

    private static void drawWatermark(Graphics2D g, String operator, LocalDateTime exportTime) {
        String watermark = operator + "  " + TIME_FMT.format(exportTime);
        Graphics2D wm = (Graphics2D) g.create();
        wm.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
        wm.setFont(watermarkFont());
        wm.setColor(C_PRIMARY);
        wm.rotate(Math.toRadians(-28), PAGE_W / 2.0, PAGE_H / 2.0);
        int stepX = 430;
        int stepY = 250;
        // 旋转后覆盖范围需大于页面本身，避免四角漏印
        for (int y = -620; y < PAGE_H + 620; y += stepY) {
            for (int x = -760; x < PAGE_W + 760; x += stepX) {
                wm.drawString(watermark, x, y);
            }
        }
        wm.dispose();
    }

    private static String bizTypeText(String bizType) {
        if (bizType == null || bizType.isEmpty()) {
            return "—";
        }
        String text = BIZ_TYPE_TEXT.get(bizType);
        return text == null ? bizType : text;
    }

    private static String shortHash(String hash) {
        if (hash == null || hash.isEmpty()) {
            return "—";
        }
        if (hash.length() <= 18) {
            return hash;
        }
        return hash.substring(0, 8) + "…" + hash.substring(hash.length() - 6);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String ellipsis(String value, FontMetrics fm, int maxWidth) {
        String s = text(value);
        if (fm.stringWidth(s) <= maxWidth) {
            return s;
        }
        int len = s.length();
        String ell = "…";
        while (len > 0 && fm.stringWidth(s.substring(0, len) + ell) > maxWidth) {
            len--;
        }
        return s.substring(0, Math.max(len, 0)) + ell;
    }

    private static List<String> wrap(String value, FontMetrics fm, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String s = text(value);
        int index = 0;
        while (index < s.length() && lines.size() < maxLines) {
            StringBuilder line = new StringBuilder();
            while (index < s.length() && fm.stringWidth(line.toString() + s.charAt(index)) <= maxWidth) {
                line.append(s.charAt(index));
                index++;
            }
            if (line.length() == 0) {
                break;
            }
            lines.add(line.toString());
        }
        if (index < s.length() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, ellipsis(lines.get(last) + s.substring(index), fm, maxWidth));
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private static Font titleFont() {
        return new Font(FONT_FAMILY, Font.BOLD, 34);
    }

    private static Font headFont() {
        return new Font(FONT_FAMILY, Font.BOLD, 15);
    }

    private static Font bodyFont() {
        return new Font(FONT_FAMILY, Font.PLAIN, 15);
    }

    private static Font smallFont() {
        return new Font(FONT_FAMILY, Font.PLAIN, 14);
    }

    private static Font hashFont() {
        return new Font(Font.MONOSPACED, Font.PLAIN, 13);
    }

    private static Font watermarkFont() {
        return new Font(FONT_FAMILY, Font.BOLD, 26);
    }

    /**
     * 选择可用的中文字体，缺失时回退到 Java 逻辑字体
     */
    private static String resolveFontFamily() {
        Set<String> available = new HashSet<>();
        try {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            available.addAll(Arrays.asList(names));
        } catch (Exception ignored) {
            // headless 或字体环境异常时按回退处理
        }
        List<String> candidates = Arrays.asList(
                "Microsoft YaHei", "微软雅黑", "SimHei", "黑体", "PingFang SC",
                "Noto Sans CJK SC", "Source Han Sans SC", "SimSun", "宋体");
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return "SansSerif";
    }

    private static class RowLayout {
        private TimestampRecord record;
        private List<String> summaryLines;
        private int height;
    }
}

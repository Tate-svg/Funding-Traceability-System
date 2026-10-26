package com.funding.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.UserContext;
import com.funding.common.UserInfo;
import com.funding.entity.TimestampRecord;
import com.funding.mapper.TimestampRecordMapper;
import com.funding.service.AuditExportService;
import com.funding.util.AuditSheetRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计溯源单据导出实现
 *
 * 水印只在导出件本身生成，普通业务导出（资金/学生/回访/评分 Excel）不经过本服务，
 * 因此普通导出不会带任何审计水印。
 */
@Service
public class AuditExportServiceImpl implements AuditExportService {

    /** 单次导出上限（防止一次导出过大） */
    private static final int MAX_RECORDS = 500;
    /** 图片导出最多展示条数（单页可读性优先，完整台账走 PDF） */
    private static final int MAX_PNG_RECORDS = 26;

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Map<String, String> BIZ_TYPE_TEXT = new HashMap<>();
    private static final Map<String, String> NODE_TEXT = new HashMap<>();

    static {
        BIZ_TYPE_TEXT.put("fund", "资助单");
        BIZ_TYPE_TEXT.put("student", "学生资料");
        BIZ_TYPE_TEXT.put("visit", "回访");
        BIZ_TYPE_TEXT.put("score", "评分");
        BIZ_TYPE_TEXT.put("score_item", "评分指标");
        BIZ_TYPE_TEXT.put("review_task", "复核任务");
        BIZ_TYPE_TEXT.put("sys_user", "用户");
        BIZ_TYPE_TEXT.put("system", "系统");

        NODE_TEXT.put("SUBMIT", "提交申请");
        NODE_TEXT.put("AUDIT_PASS", "审核通过");
        NODE_TEXT.put("AUDIT_REJECT", "审核驳回");
        NODE_TEXT.put("REVIEW_DRAW", "大额抽检");
        NODE_TEXT.put("REVIEW", "复核确认");
        NODE_TEXT.put("DISBURSE", "资金拨付");
        NODE_TEXT.put("VOUCHER_MODIFY", "凭证修改");
        NODE_TEXT.put("VISIT_RECORD", "回访录入");
        NODE_TEXT.put("STUDENT_UPLOAD", "资料上传");
    }

    @Autowired
    private TimestampRecordMapper timestampRecordMapper;

    @Override
    public void export(HttpServletResponse response, String format, String bizType, Long bizId,
                       String node, String keyword) throws Exception {
        boolean asPdf = !"png".equalsIgnoreCase(format);
        LocalDateTime exportTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        UserInfo user = UserContext.get();
        String operator = (user == null || user.getUsername() == null || user.getUsername().isEmpty())
                ? "unknown" : user.getUsername();

        Page<TimestampRecord> query = new Page<>(1, MAX_RECORDS);
        IPage<TimestampRecord> result = timestampRecordMapper.selectPage(query, bizType, bizId, node, keyword);
        List<TimestampRecord> records = result.getRecords() == null
                ? new ArrayList<TimestampRecord>() : new ArrayList<>(result.getRecords());
        long total = result.getTotal();

        String filterDesc = buildFilterDesc(bizType, bizId, node, keyword, total, records.size());
        List<TimestampRecord> target = records;
        if (!asPdf && records.size() > MAX_PNG_RECORDS) {
            target = new ArrayList<>(records.subList(0, MAX_PNG_RECORDS));
            filterDesc += "（图片仅展示前 " + MAX_PNG_RECORDS + " 条，完整台账请导出 PDF）";
        }

        List<BufferedImage> pages = AuditSheetRenderer.render(target, operator, exportTime, filterDesc);
        if (pages.isEmpty()) {
            pages = Collections.singletonList(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        }

        String fileName = "审计溯源单_" + FILE_FMT.format(exportTime) + (asPdf ? ".pdf" : ".png");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

        if (asPdf) {
            writePdf(response, pages);
        } else {
            response.setContentType("image/png");
            ImageIO.write(pages.get(0), "png", response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    private void writePdf(HttpServletResponse response, List<BufferedImage> pages) throws IOException {
        response.setContentType("application/pdf");
        try (PDDocument document = new PDDocument()) {
            for (BufferedImage image : pages) {
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, toJpeg(image), "audit-trace");
                PDPage page = new PDPage(new PDRectangle(595.28f, 841.89f));
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.drawImage(pdImage, 0, 0, 595.28f, 841.89f);
                }
            }
            document.save(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    private byte[] toJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.92f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private String buildFilterDesc(String bizType, Long bizId, String node, String keyword,
                                   long total, int exported) {
        StringBuilder sb = new StringBuilder();
        sb.append("业务类型=").append(bizType == null || bizType.isEmpty()
                ? "全部" : BIZ_TYPE_TEXT.getOrDefault(bizType, bizType));
        if (bizId != null) {
            sb.append("，单据ID=").append(bizId);
        }
        sb.append("，节点=").append(node == null || node.isEmpty()
                ? "全部" : NODE_TEXT.getOrDefault(node, node));
        if (keyword != null && !keyword.isEmpty()) {
            sb.append("，关键词=").append(keyword);
        }
        if (total > exported) {
            sb.append("（共 ").append(total).append(" 条，仅导出最近 ").append(exported).append(" 条）");
        }
        return sb.toString();
    }
}

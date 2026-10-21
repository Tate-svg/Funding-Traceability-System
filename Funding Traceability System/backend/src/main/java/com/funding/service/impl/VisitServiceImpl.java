package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.BusinessException;
import com.funding.common.MaskingGuard;
import com.funding.common.UserContext;
import com.funding.entity.Fund;
import com.funding.entity.Visit;
import com.funding.mapper.FundMapper;
import com.funding.mapper.VisitMapper;
import com.funding.service.TimestampService;
import com.funding.service.VisitService;
import com.funding.util.ExcelUtils;
import com.funding.util.MaskUtils;
import com.funding.vo.VisitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 回访服务实现
 * - SPONSOR 查询一律按当前登录 userId 过滤 sponsor_id
 * - 新增/修改时自动生成脱敏回访记录（masked_record）
 * - 凭证材料上传时自动脱敏：文本类内容正则脱敏；图片类重编码剥离 EXIF 元数据
 */
@Slf4j
@Service
public class VisitServiceImpl implements VisitService {

    /** 文本类材料扩展名（内容正则脱敏） */
    private static final Set<String> TEXT_EXT = new HashSet<>(Arrays.asList("txt", "md", "csv", "log"));
    /** 图片类材料扩展名（ImageIO 重编码剥离元数据） */
    private static final Set<String> IMAGE_EXT = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "bmp"));
    /** 允许上传的扩展名（照片/访谈记录等核验必要材料） */
    private static final Set<String> ALLOW_EXT = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "bmp", "gif", "webp", "pdf", "txt", "md", "csv", "log", "doc", "docx"));

    @Autowired
    private VisitMapper visitMapper;

    @Autowired
    private FundMapper fundMapper;

    @Autowired
    private MaskingGuard maskingGuard;

    @Autowired
    private TimestampService timestampService;

    /** 脱敏凭证材料存储目录（与 upload/attachments 同根） */
    @Value("${funding.upload.dir:upload/visit-material}")
    private String uploadDir;

    @Override
    public IPage<VisitVO> page(Integer page, Integer size, Long studentId, Long fundId, String keyword) {
        Page<VisitVO> p = new Page<>(page, size);
        Long sponsorId = null;
        if ("SPONSOR".equals(UserContext.getRole())) {
            sponsorId = UserContext.getUserId();
        }
        return visitMapper.selectPageWithNames(p, keyword, studentId, sponsorId, fundId);
    }

    @Override
    public VisitVO getById(Long id) {
        VisitVO visit = visitMapper.selectByIdWithNames(id);
        if (visit == null) {
            return null;
        }
        checkSponsorOwnership(visit.getSponsorId());
        return visit;
    }

    @Override
    public void saveVisit(Visit visit) {
        visit.setId(null);
        validateFundBinding(visit);
        // 二次脱敏校验：回访自由文本不得夹带未打码敏感信息
        maskingGuard.check("POST /api/visits", "回访内容", visit.getContent());
        maskingGuard.check("POST /api/visits", "情况说明", visit.getSituation());
        maskingGuard.check("POST /api/visits", "存在问题", visit.getProblem());
        // 自动生成脱敏回访记录（数据最小采集，只保留核验必要信息）
        visit.setMaskedRecord(MaskUtils.maskRecord(visit.getContent(), visit.getSituation(), visit.getProblem()));
        // 凭证文件名只能由上传接口回写，防止客户端伪造
        visit.setMaskedMaterial(null);
        visitMapper.insert(visit);
        // 回访节点同步写入对应资助流水的时间戳链，形成 资助-拨付-回访 闭环
        stampFundNode("新增回访", visit.getFundId());
    }

    @Override
    public void updateVisit(Visit visit) {
        validateFundBinding(visit);
        maskingGuard.check("PUT /api/visits/" + visit.getId(), "回访内容", visit.getContent());
        maskingGuard.check("PUT /api/visits/" + visit.getId(), "情况说明", visit.getSituation());
        maskingGuard.check("PUT /api/visits/" + visit.getId(), "存在问题", visit.getProblem());
        visit.setMaskedRecord(MaskUtils.maskRecord(visit.getContent(), visit.getSituation(), visit.getProblem()));
        // 置空后 MyBatis-Plus 默认忽略 null 字段，不会覆盖已上传的凭证
        visit.setMaskedMaterial(null);
        visitMapper.updateById(visit);
        stampFundNode("修改回访", visit.getFundId());
    }

    @Override
    public void deleteVisit(Long id) {
        visitMapper.deleteById(id);
    }

    @Override
    public String uploadMaterial(Long id, MultipartFile file, boolean manuallyMasked) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        Visit visit = visitMapper.selectById(id);
        if (visit == null) {
            throw new BusinessException(404, "回访记录不存在");
        }
        if (visit.getFundId() == null) {
            throw new BusinessException(400, "该回访未绑定资助流水，无法上传凭证");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOW_EXT.contains(ext)) {
            throw new BusinessException(400, "仅支持上传照片/访谈记录类材料：" + ALLOW_EXT);
        }
        // 文本类由系统自动脱敏；图片/文档类无法自动识别画面内容，须上传前人工脱敏并确认
        if (!TEXT_EXT.contains(ext) && !manuallyMasked) {
            throw new BusinessException(400, "图片/文档类凭证请先人工脱敏（遮挡身份证号、手机号、住址等），勾选确认后再上传");
        }

        String storedName = "masked_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(storedName).normalize();
        if (!target.startsWith(dir)) {
            throw new BusinessException(400, "非法文件名");
        }

        byte[] bytes = file.getBytes();
        if (TEXT_EXT.contains(ext)) {
            // 文本类材料：内容正则脱敏后存储
            String masked = MaskUtils.mask(new String(bytes, StandardCharsets.UTF_8));
            Files.write(target, masked.getBytes(StandardCharsets.UTF_8));
        } else if (IMAGE_EXT.contains(ext)) {
            // 图片类材料：重编码剥离 EXIF 元数据（拍摄设备/GPS 等隐私信息不保留）
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BusinessException(400, "图片文件无法解析");
            }
            String format = ext.equals("jpg") ? "jpeg" : ext;
            if ("jpeg".equals(format) && image.getColorModel().hasAlpha()) {
                // JPEG 不支持透明通道，转为 RGB
                BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgb.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
                image = rgb;
            }
            ImageIO.write(image, format, target.toFile());
        } else {
            // 其他类型（PDF/Office 等）：按脱敏副本统一命名存储
            Files.write(target, bytes);
        }

        // 条件更新，仅回写脱敏材料字段
        UpdateWrapper<Visit> uw = new UpdateWrapper<>();
        uw.eq("id", id).set("masked_material", storedName);
        visitMapper.update(null, uw);
        // 凭证上传节点写入对应资助流水的时间戳链
        stampFundNode("上传回访脱敏凭证", visit.getFundId());
        return storedName;
    }

    @Override
    public void downloadMaterial(Long id, HttpServletResponse response) throws Exception {
        VisitVO visit = visitMapper.selectByIdWithNames(id);
        if (visit == null) {
            throw new BusinessException(404, "回访记录不存在");
        }
        checkSponsorOwnership(visit.getSponsorId());
        if (visit.getMaskedMaterial() == null || visit.getMaskedMaterial().isEmpty()) {
            throw new BusinessException(404, "该回访暂无脱敏凭证材料");
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = dir.resolve(visit.getMaskedMaterial()).normalize();
        if (!file.startsWith(dir) || !Files.exists(file)) {
            throw new BusinessException(404, "凭证文件不存在");
        }
        String ext = visit.getMaskedMaterial().substring(visit.getMaskedMaterial().lastIndexOf('.') + 1);
        String contentType = Files.probeContentType(file);
        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("回访脱敏凭证." + ext, "UTF-8"));
        Files.copy(file, response.getOutputStream());
        response.getOutputStream().flush();
    }

    @Override
    public void export(HttpServletResponse response) throws Exception {
        List<VisitVO> list = visitMapper.selectAllWithNames();
        List<String> headers = Arrays.asList(
                "ID", "学生", "资助人", "资助凭证号", "回访日期", "回访内容", "回访情况", "问题记录",
                "脱敏回访记录", "脱敏凭证材料", "回访人", "创建时间");
        List<List<Object>> rows = new ArrayList<>();
        for (VisitVO v : list) {
            List<Object> row = new ArrayList<>();
            row.add(v.getId());
            row.add(v.getStudentName());
            row.add(v.getSponsorName());
            row.add(v.getVoucherNo());
            row.add(v.getVisitDate() != null ? v.getVisitDate().toString() : "");
            row.add(v.getContent());
            row.add(v.getSituation());
            row.add(v.getProblem());
            row.add(v.getMaskedRecord());
            row.add(v.getMaskedMaterial());
            row.add(v.getVisitor());
            row.add(v.getCreateTime() != null ? v.getCreateTime().toString() : "");
            rows.add(row);
        }
        ExcelUtils.writeWorkbook(response, "回访记录.xlsx", headers, rows);
    }

    /**
     * 校验资助流水绑定：必须绑定，且单据存在、与回访学生一致
     */
    private void validateFundBinding(Visit visit) {
        if (visit.getFundId() == null) {
            throw new BusinessException(400, "请先绑定资助流水号");
        }
        Fund fund = fundMapper.selectById(visit.getFundId());
        if (fund == null) {
            throw new BusinessException(400, "绑定的资助流水不存在");
        }
        if (visit.getStudentId() != null && !visit.getStudentId().equals(fund.getStudentId())) {
            throw new BusinessException(400, "资助流水与回访学生不一致");
        }
    }

    /**
     * 将回访节点同步写入对应资助流水的时间戳链；失败仅告警，不影响主业务
     */
    private void stampFundNode(String opValue, Long fundId) {
        if (fundId == null) {
            return;
        }
        try {
            timestampService.stamp(opValue, "fund", fundId, null);
        } catch (Exception e) {
            log.warn("回访节点写入资助时间戳链失败: {}", e.getMessage());
        }
    }

    /**
     * 数据隔离：SPONSOR 只能访问自己名下回访
     */
    private void checkSponsorOwnership(Long sponsorId) {
        if ("SPONSOR".equals(UserContext.getRole())
                && (sponsorId == null || !sponsorId.equals(UserContext.getUserId()))) {
            throw new BusinessException(403, "权限不足");
        }
    }
}

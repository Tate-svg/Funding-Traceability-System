package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.funding.common.BusinessException;
import com.funding.entity.StudentAttachment;
import com.funding.mapper.StudentAttachmentMapper;
import com.funding.service.StudentAttachmentService;
import com.funding.util.MaskingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生证明材料实现
 * 上传仅支持 .txt 文本材料（演示版）；上传即进入脱敏预览态，
 * 自动识别敏感片段并给出打码建议，用户确认后的打码文本入库
 */
@Service
public class StudentAttachmentServiceImpl implements StudentAttachmentService {

    private static final String UPLOAD_ROOT = "upload/attachments";

    @Autowired
    private StudentAttachmentMapper attachmentMapper;

    @Override
    public Map<String, Object> upload(Long studentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件为空");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".txt")) {
            throw new BusinessException(400, "演示版仅支持 .txt 文本材料");
        }
        String text;
        String storedPath;
        try {
            byte[] bytes = file.getBytes();
            text = new String(bytes, StandardCharsets.UTF_8);
            Path dir = Paths.get(UPLOAD_ROOT, String.valueOf(studentId));
            Files.createDirectories(dir);
            String storedName = System.currentTimeMillis() + "_" + name;
            Path target = dir.resolve(storedName);
            Files.write(target, bytes);
            storedPath = target.toString();
        } catch (Exception e) {
            throw new BusinessException(500, "材料保存失败，请重试");
        }

        StudentAttachment att = new StudentAttachment();
        att.setStudentId(studentId);
        att.setFileName(name);
        att.setFilePath(storedPath);
        att.setMaskStatus(StudentAttachment.STATUS_PENDING);
        attachmentMapper.insert(att);

        Map<String, Object> result = new HashMap<>();
        result.put("attachmentId", att.getId());
        result.put("originalText", text);
        result.put("hits", MaskingEngine.scan(text));
        return result;
    }

    @Override
    public void confirm(Long studentId, Long attachmentId, String maskedText) {
        StudentAttachment att = attachmentMapper.selectById(attachmentId);
        if (att == null || !att.getStudentId().equals(studentId)) {
            throw new BusinessException(404, "材料不存在");
        }
        List<MaskingEngine.Hit> hits = MaskingEngine.scan(maskedText);
        if (!hits.isEmpty()) {
            MaskingEngine.Hit first = hits.get(0);
            throw new BusinessException(400, "仍有未脱敏敏感内容（" + first.getRule() + " "
                    + first.getMasked() + "），请完成打码后重新确认");
        }
        att.setMaskedText(maskedText);
        att.setMaskStatus(StudentAttachment.STATUS_CONFIRMED);
        attachmentMapper.updateById(att);
    }

    @Override
    public List<StudentAttachment> listByStudent(Long studentId) {
        return attachmentMapper.selectList(new QueryWrapper<StudentAttachment>()
                .eq("student_id", studentId).orderByDesc("id"));
    }
}

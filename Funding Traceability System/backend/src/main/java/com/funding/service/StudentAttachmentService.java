package com.funding.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 学生证明材料服务（上传 -> 脱敏预览 -> 确认入库）
 */
public interface StudentAttachmentService {

    /**
     * 上传材料：存文件 + 建 PENDING 记录 + 脱敏引擎扫描
     * 返回 {attachmentId, originalText, hits:[{start,end,rule,text,masked}]}
     */
    Map<String, Object> upload(Long studentId, MultipartFile file);

    /**
     * 确认打码文本：仍含未打码敏感内容则拒绝
     */
    void confirm(Long studentId, Long attachmentId, String maskedText);

    /** 某学生的材料列表 */
    List<com.funding.entity.StudentAttachment> listByStudent(Long studentId);
}

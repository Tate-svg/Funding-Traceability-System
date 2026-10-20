package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生证明材料（上传后进入脱敏预览态，确认打码文本后入库）
 */
@Data
@TableName("student_attachment")
public class StudentAttachment {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private String fileName;

    private String filePath;

    /** PENDING 待脱敏确认 / CONFIRMED 已确认 */
    private String maskStatus;

    /** 用户确认后的打码文本 */
    private String maskedText;

    private LocalDateTime createTime;
}

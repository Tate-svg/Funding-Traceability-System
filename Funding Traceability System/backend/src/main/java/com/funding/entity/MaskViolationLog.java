package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 脱敏拦截记录实体（只读存证：仅 insert，无 update/delete）
 */
@Data
@TableName("mask_violation_log")
public class MaskViolationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** 被拦截的接口，如 POST /api/funds */
    private String api;

    /** 命中字段，如 备注 */
    private String field;

    /** 命中规则：ID_CARD / PHONE / BANK_CARD / LONG_DIGIT */
    private String rule;

    /** 命中片段（仅存打码后形式，不落明文） */
    private String hitText;

    private LocalDateTime createTime;
}

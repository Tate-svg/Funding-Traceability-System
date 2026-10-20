package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 隐私明文调取审批单（两级审批流：审核管理员初审 -> 监事终审）
 */
@Data
@TableName("privacy_access_apply")
public class PrivacyAccessApply {

    public static final String STATUS_PENDING_L1 = "PENDING_L1";
    public static final String STATUS_PENDING_L2 = "PENDING_L2";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantId;

    private String applicantName;

    private String virtualId;

    /** 调取字段范围，逗号分隔：name,idCard,phone,familyAddress,familySituation */
    private String fieldScope;

    private String reason;

    private String status;

    private Long l1By;

    private LocalDateTime l1Time;

    private String l1Remark;

    private Long l2By;

    private LocalDateTime l2Time;

    private String l2Remark;

    /** 终审通过后的明文可见截止时间 */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}

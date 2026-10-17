package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资金流水实体
 * 状态机：SUBMITTED → AUDITED(驳回 REJECTED) → REVIEWED → DISBURSED
 */
@Data
@TableName("fund")
public class Fund {

    /** 已提交，待审核 */
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    /** 审核通过（大额待抽检/小额待拨付） */
    public static final String STATUS_AUDITED = "AUDITED";
    /** 审核驳回 */
    public static final String STATUS_REJECTED = "REJECTED";
    /** 复核通过（含抽检轮空放行），待拨付 */
    public static final String STATUS_REVIEWED = "REVIEWED";
    /** 已拨付 */
    public static final String STATUS_DISBURSED = "DISBURSED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long sponsorId;

    private BigDecimal amount;

    private LocalDate fundDate;

    private String purpose;

    private String remark;

    private String status;

    /** 资助凭证号：Voucher-yyyyMMdd-4位序号，审核通过时生成 */
    private String voucherNo;

    /** 关键字段 SHA-256 存证 */
    private String voucherHash;

    private Long auditBy;

    private LocalDateTime auditTime;

    private String auditRemark;

    private Long disburseBy;

    private LocalDateTime disburseTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

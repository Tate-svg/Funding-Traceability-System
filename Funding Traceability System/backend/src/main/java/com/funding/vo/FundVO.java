package com.funding.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资金流水查询返回对象（含学生姓名、资助人姓名、状态机与凭证字段）
 */
@Data
public class FundVO {

    private Long id;

    private Long studentId;

    private Long sponsorId;

    private BigDecimal amount;

    private LocalDate fundDate;

    private String purpose;

    private String remark;

    private String status;

    private String voucherNo;

    private String voucherHash;

    private Long auditBy;

    private LocalDateTime auditTime;

    private String auditRemark;

    private Long disburseBy;

    private LocalDateTime disburseTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String studentName;

    private String sponsorName;

    /** 审核人姓名 */
    private String auditorName;

    /** 拨付人姓名 */
    private String disburserName;
}

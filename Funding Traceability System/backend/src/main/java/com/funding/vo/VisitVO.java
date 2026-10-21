package com.funding.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回访记录查询返回对象（含学生姓名、资助人姓名）
 */
@Data
public class VisitVO {

    private Long id;

    private Long studentId;

    private Long sponsorId;

    /** 绑定的资助流水号（fund.id） */
    private Long fundId;

    /** 资助凭证号（联表 fund.voucher_no） */
    private String voucherNo;

    private LocalDate visitDate;

    private String content;

    private String situation;

    private String problem;

    private String visitor;

    /** 脱敏回访记录 */
    private String maskedRecord;

    /** 脱敏影像/凭证材料文件名 */
    private String maskedMaterial;

    private LocalDateTime createTime;

    private String studentName;

    private String sponsorName;
}

package com.funding.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 复核任务查询返回对象（含资金单与学生信息）
 */
@Data
public class ReviewTaskVO {

    private Long id;

    private Long fundId;

    private String batchNo;

    private BigDecimal sampleRatio;

    private String status;

    private Long reviewerId;

    private String reviewRemark;

    private LocalDateTime createTime;

    private LocalDateTime reviewTime;

    private BigDecimal amount;

    private String voucherNo;

    private String fundStatus;

    private String studentName;

    private String sponsorName;
}

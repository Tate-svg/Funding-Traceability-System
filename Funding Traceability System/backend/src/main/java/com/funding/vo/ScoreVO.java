package com.funding.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评分记录查询返回对象（含学生姓名、资助人姓名、评分明细）
 * POST/PUT 评分时作为请求体：包含 details 列表
 */
@Data
public class ScoreVO {

    private Long id;

    private Long studentId;

    private String studentName;

    private Long sponsorId;

    private String sponsorName;

    private Long fundId;

    private String fundNo;

    private String ruleVersion;

    private LocalDateTime ruleCapturedAt;

    private LocalDate scoreDate;

    private BigDecimal totalScore;

    private String remark;

    private LocalDateTime createTime;

    private List<ScoreDetailVO> details;
}

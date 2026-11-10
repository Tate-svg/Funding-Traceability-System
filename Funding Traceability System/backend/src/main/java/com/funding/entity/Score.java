package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评分记录实体
 */
@Data
@TableName("score")
public class Score {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long sponsorId;

    private Long fundId;

    /** 服务端生成的规则摘要；null表示历史记录未留存规则。 */
    private String ruleVersion;

    private LocalDateTime ruleCapturedAt;

    private LocalDate scoreDate;

    private BigDecimal totalScore;

    private String remark;

    private LocalDateTime createTime;
}

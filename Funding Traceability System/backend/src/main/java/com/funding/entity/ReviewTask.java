package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 大额复核抽检任务
 * 状态：PENDING 待复核 / PASS 复核通过 / FAIL 复核不通过
 */
@Data
@TableName("review_task")
public class ReviewTask {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_FAIL = "FAIL";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fundId;

    /** 抽检批次号：B+yyyyMMddHHmmss */
    private String batchNo;

    /** 本次抽检比例(%) */
    private BigDecimal sampleRatio;

    private String status;

    private Long reviewerId;

    private String reviewRemark;

    private LocalDateTime createTime;

    private LocalDateTime reviewTime;
}

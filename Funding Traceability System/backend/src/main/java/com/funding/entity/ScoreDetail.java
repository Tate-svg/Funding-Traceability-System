package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 评分明细实体
 */
@Data
@TableName("score_detail")
public class ScoreDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scoreId;

    private Long itemId;

    private BigDecimal itemScore;

    private String itemNameSnapshot;

    private BigDecimal weightSnapshot;
}

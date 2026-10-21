package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回访记录实体
 */
@Data
@TableName("visit")
public class Visit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long sponsorId;

    /** 绑定的资助流水号（fund.id），形成 资助-拨付-回访 闭环 */
    private Long fundId;

    private LocalDate visitDate;

    private String content;

    private String situation;

    private String problem;

    private String visitor;

    /** 脱敏回访记录：content/situation/problem 自动脱敏后合并生成 */
    private String maskedRecord;

    /** 脱敏影像/凭证材料：上传时自动脱敏后的存储文件名 */
    private String maskedMaterial;

    private LocalDateTime createTime;
}

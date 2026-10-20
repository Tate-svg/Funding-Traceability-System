package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生实体（业务库）
 * 只含虚拟ID + 脱敏名 + 非敏感字段；隐私明文加密存于隐私库 privacy_profile
 */
@Data
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 虚拟ID：与隐私库 privacy_profile.virtual_id 一对一映射 */
    private String virtualId;

    /** 脱敏展示名，如 李** */
    private String maskName;

    private String gender;

    private String school;

    private String grade;

    private Long sponsorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

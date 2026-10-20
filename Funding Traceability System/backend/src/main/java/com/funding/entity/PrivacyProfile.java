package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生隐私档案实体（隐私库 funding_privacy.privacy_profile）
 * 五个敏感字段均为 AES Base64 密文；业务库仅通过 virtual_id 映射
 */
@Data
@TableName("privacy_profile")
public class PrivacyProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String virtualId;

    /** 姓名（密文） */
    private String nameEnc;

    /** 身份证号（密文） */
    private String idCardEnc;

    /** 联系电话（密文） */
    private String phoneEnc;

    /** 家庭住址（密文） */
    private String familyAddressEnc;

    /** 家庭情况（密文） */
    private String familySituationEnc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

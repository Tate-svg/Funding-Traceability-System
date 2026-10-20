package com.funding.vo;

import lombok.Data;

/**
 * 学生新增/修改请求
 * 隐私字段加密后写入隐私库 privacy_profile，业务库只落虚拟ID与脱敏名
 */
@Data
public class StudentSaveRequest {

    private String gender;

    private String school;

    private String grade;

    private Long sponsorId;

    // ---- 隐私字段（AES 加密入 funding_privacy.privacy_profile） ----

    private String name;

    private String idCard;

    private String phone;

    private String familyAddress;

    private String familySituation;
}

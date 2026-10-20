package com.funding.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生查询返回对象（业务库脱敏视图，含资助人姓名）
 * 默认只含脱敏数据；明文需经隐私调取审批后由 /api/privacy/profile/{virtualId} 单独返回
 */
@Data
public class StudentVO {

    private Long id;

    private String virtualId;

    private String maskName;

    private String gender;

    private String school;

    private String grade;

    private Long sponsorId;

    private String sponsorName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

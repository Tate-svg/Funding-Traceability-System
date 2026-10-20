package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 隐私明文调取留痕（只读存证：仅 insert，无 update/delete）
 */
@Data
@TableName("privacy_access_log")
public class PrivacyAccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的审批单号 */
    private Long applyId;

    private Long userId;

    private String username;

    private String virtualId;

    /** 本次实际调取的字段 */
    private String fields;

    private LocalDateTime createTime;
}

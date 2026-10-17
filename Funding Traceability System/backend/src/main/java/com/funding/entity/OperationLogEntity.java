package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体（只读存证：仅 insert / select，无 update / delete）
 */
@Data
@TableName("operation_log")
public class OperationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String role;

    /** 操作描述，如"审核资助申请" */
    private String operation;

    /** 目标类型，如 fund / review_task / student */
    private String targetType;

    private Long targetId;

    /** 操作内容摘要 */
    private String content;

    private LocalDateTime createTime;
}

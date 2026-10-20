package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 可信时间戳台账实体（只读存证：仅 insert / select，无 update / delete）
 * 每条业务节点记录一对一绑定时间戳，全局序列号 + 哈希链固化时序
 */
@Data
@TableName("timestamp_record")
public class TimestampRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 全局时序号，递增，固化全链路顺序 */
    private Long serialNo;

    /** 业务类型：fund / student / visit / score / review_task / sys_user */
    private String bizType;

    private Long bizId;

    /** 业务节点编码：SUBMIT/AUDIT_PASS/AUDIT_REJECT/REVIEW/REVIEW_DRAW/DISBURSE/VOUCHER_MODIFY/VISIT_RECORD/STUDENT_UPLOAD/OTHER */
    private String node;

    /** 节点显示名，如"提交申请" */
    private String nodeName;

    /** 业务摘要 */
    private String summary;

    private Long operatorId;

    private String operatorName;

    private String role;

    /** 时间戳时刻（本地模拟 TSA 盖章时间） */
    private LocalDateTime tsTime;

    /** 本条哈希 SHA-256(prev_hash|serial|biz|node|ts_time|operator|summary) */
    private String tsHash;

    /** 链上前一条哈希 */
    private String prevHash;

    /** 1=时序倒序预警 */
    private Integer anomaly;

    private String anomalyReason;

    private LocalDateTime createTime;
}

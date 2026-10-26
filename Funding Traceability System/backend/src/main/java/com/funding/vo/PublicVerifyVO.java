package com.funding.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 扫码公开核验结果
 *
 * 隐私红线：仅允许返回业务单号、资助金额、审批节点时间与业务状态，
 * 严禁返回学生姓名/学校/年级、资助人、用途、备注、操作人等任何可关联到个人的信息。
 */
@Data
public class PublicVerifyVO {

    /** 业务单号（凭证号优先，未生成时返回单据编号） */
    private String businessNo;

    /** 资助金额 */
    private BigDecimal amount;

    /** 业务状态码 */
    private String status;

    /** 业务状态中文 */
    private String statusText;

    /** 审核是否完成 */
    private Boolean auditCompleted;

    /** 拨付是否完成 */
    private Boolean disburseCompleted;

    /** 审批节点（提交 / 审核 / 复核 / 拨付） */
    private List<Node> nodes;

    /** 本次核验时间（数据实时联动，每次请求返回最新状态） */
    private LocalDateTime verifyTime;

    @Data
    public static class Node {

        /** 节点编码：SUBMIT / AUDIT / REVIEW / DISBURSE */
        private String node;

        /** 节点名称（不含操作人） */
        private String nodeName;

        /** DONE 已完成 / PENDING 待处理 / SKIPPED 无需复核 */
        private String state;

        /** 节点时间戳（未完成时为空） */
        private LocalDateTime time;
    }
}

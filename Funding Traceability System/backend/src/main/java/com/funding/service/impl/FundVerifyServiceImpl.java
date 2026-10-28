package com.funding.service.impl;

import com.funding.common.BusinessException;
import com.funding.common.UserContext;
import com.funding.common.UserInfo;
import com.funding.common.VerifyCodeService;
import com.funding.entity.Fund;
import com.funding.entity.TimestampRecord;
import com.funding.mapper.FundMapper;
import com.funding.mapper.TimestampRecordMapper;
import com.funding.service.FundVerifyService;
import com.funding.util.QrCodeUtils;
import com.funding.vo.FundQrCodeVO;
import com.funding.vo.PublicVerifyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资助凭证溯源核验服务实现
 *
 * 核验页数据全部来自主业务表与只读时间戳台账，不做任何缓存，
 * 单据状态更新后扫码内容即时同步（实时联动）。
 */
@Service
public class FundVerifyServiceImpl implements FundVerifyService {

    private static final String NODE_SUBMIT = "SUBMIT";
    private static final String NODE_AUDIT_PASS = "AUDIT_PASS";
    private static final String NODE_AUDIT_REJECT = "AUDIT_REJECT";
    private static final String NODE_REVIEW = "REVIEW";
    private static final String NODE_DISBURSE = "DISBURSE";

    private static final String STATE_DONE = "DONE";
    private static final String STATE_PENDING = "PENDING";
    private static final String STATE_SKIPPED = "SKIPPED";

    @Autowired
    private FundMapper fundMapper;

    @Autowired
    private TimestampRecordMapper timestampRecordMapper;

    @Autowired
    private VerifyCodeService verifyCodeService;

    @Value("${verify.page-base-url:http://localhost:8081/#/verify}")
    private String pageBaseUrl;

    @Value("${funding.review.threshold:5000}")
    private BigDecimal reviewThreshold;

    @Override
    public FundQrCodeVO qrCode(Long fundId) {
        Fund fund = fundMapper.selectById(fundId);
        if (fund == null) {
            throw new BusinessException(404, "单据不存在");
        }
        // 权限隔离：资助人仅可为本人名下单据生成核验二维码
        UserInfo user = UserContext.get();
        if (user != null && "SPONSOR".equals(user.getRole())
                && !user.getUserId().equals(fund.getSponsorId())) {
            throw new BusinessException(403, "权限不足");
        }

        String code = verifyCodeService.encode(fundId);
        String verifyUrl = pageBaseUrl + "/" + code;

        FundQrCodeVO vo = new FundQrCodeVO();
        vo.setFundId(fund.getId());
        vo.setBusinessNo(businessNo(fund));
        vo.setVerifyCode(code);
        vo.setVerifyUrl(verifyUrl);
        vo.setQrImage(QrCodeUtils.toPngDataUrl(verifyUrl, 280));
        vo.setStatus(fund.getStatus());
        vo.setStatusText(statusText(fund.getStatus()));
        return vo;
    }

    @Override
    public PublicVerifyVO publicVerify(String code) {
        Long fundId = verifyCodeService.decode(code);
        if (fundId == null) {
            throw new BusinessException(404, "核验码无效，请核对二维码来源");
        }
        Fund fund = fundMapper.selectById(fundId);
        if (fund == null) {
            throw new BusinessException(404, "单据不存在或已删除");
        }

        String status = fund.getStatus();
        BigDecimal amount = fund.getAmount() == null ? BigDecimal.ZERO : fund.getAmount();
        Map<String, LocalDateTime> stamps = nodeTimes(fund);

        PublicVerifyVO vo = new PublicVerifyVO();
        vo.setBusinessNo(businessNo(fund));
        vo.setAmount(amount);
        vo.setStatus(status);
        vo.setStatusText(statusText(status));
        vo.setAuditCompleted(Fund.STATUS_AUDITED.equals(status)
                || Fund.STATUS_REVIEWED.equals(status)
                || Fund.STATUS_DISBURSED.equals(status));
        vo.setDisburseCompleted(Fund.STATUS_DISBURSED.equals(status));
        vo.setNodes(buildNodes(fund, amount, stamps));
        vo.setVerifyTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return vo;
    }

    /**
     * 各节点时间：优先取只读时间戳台账，缺失时回退主业务字段
     */
    private Map<String, LocalDateTime> nodeTimes(Fund fund) {
        Map<String, LocalDateTime> stamps = new HashMap<>();
        List<TimestampRecord> records = timestampRecordMapper.selectListByBiz("fund", fund.getId());
        if (records != null) {
            for (TimestampRecord record : records) {
                if (record.getNode() != null && record.getTsTime() != null) {
                    stamps.putIfAbsent(record.getNode(), record.getTsTime());
                }
            }
        }
        if (!stamps.containsKey(NODE_SUBMIT) && fund.getCreateTime() != null) {
            stamps.put(NODE_SUBMIT, fund.getCreateTime());
        }
        if (!stamps.containsKey(NODE_AUDIT_PASS) && !stamps.containsKey(NODE_AUDIT_REJECT)
                && fund.getAuditTime() != null) {
            stamps.put(Fund.STATUS_REJECTED.equals(fund.getStatus()) ? NODE_AUDIT_REJECT : NODE_AUDIT_PASS,
                    fund.getAuditTime());
        }
        if (!stamps.containsKey(NODE_DISBURSE) && fund.getDisburseTime() != null) {
            stamps.put(NODE_DISBURSE, fund.getDisburseTime());
        }
        return stamps;
    }

    private List<PublicVerifyVO.Node> buildNodes(Fund fund, BigDecimal amount, Map<String, LocalDateTime> stamps) {
        String status = fund.getStatus();
        List<PublicVerifyVO.Node> nodes = new ArrayList<>();

        LocalDateTime submitTime = stamps.get(NODE_SUBMIT);
        nodes.add(node(NODE_SUBMIT, "提交申请", submitTime == null ? STATE_PENDING : STATE_DONE, submitTime));

        LocalDateTime auditTime = stamps.get(NODE_AUDIT_PASS);
        String auditName = "审核";
        String auditState = STATE_PENDING;
        if (Fund.STATUS_REJECTED.equals(status)) {
            auditTime = stamps.get(NODE_AUDIT_REJECT);
            auditName = "审核驳回";
            auditState = auditTime == null ? STATE_PENDING : STATE_DONE;
        } else if (auditTime != null) {
            auditName = "审核通过";
            auditState = STATE_DONE;
        }
        nodes.add(node("AUDIT", auditName, auditState, auditTime));

        LocalDateTime reviewTime = stamps.get(NODE_REVIEW);
        String reviewState;
        if (reviewTime != null) {
            reviewState = STATE_DONE;
        } else if (amount.compareTo(threshold()) < 0) {
            // 小额资助免复核，避免核验页出现「永远待处理」的误导
            reviewState = STATE_SKIPPED;
        } else {
            reviewState = STATE_PENDING;
        }
        nodes.add(node("REVIEW", "复核", reviewState, reviewTime));

        LocalDateTime disburseTime = stamps.get(NODE_DISBURSE);
        nodes.add(node("DISBURSE", "资金拨付", disburseTime == null ? STATE_PENDING : STATE_DONE, disburseTime));

        return nodes;
    }

    private PublicVerifyVO.Node node(String code, String name, String state, LocalDateTime time) {
        PublicVerifyVO.Node node = new PublicVerifyVO.Node();
        node.setNode(code);
        node.setNodeName(name);
        node.setState(state);
        node.setTime(time);
        return node;
    }

    private BigDecimal threshold() {
        return reviewThreshold == null ? new BigDecimal("5000") : reviewThreshold;
    }

    private String businessNo(Fund fund) {
        if (fund.getVoucherNo() != null && !fund.getVoucherNo().isEmpty()) {
            return fund.getVoucherNo();
        }
        return "FUND-" + fund.getId();
    }

    private String statusText(String status) {
        if (Fund.STATUS_SUBMITTED.equals(status)) {
            return "待审核";
        }
        if (Fund.STATUS_AUDITED.equals(status)) {
            return "审核通过";
        }
        if (Fund.STATUS_REJECTED.equals(status)) {
            return "已驳回";
        }
        if (Fund.STATUS_REVIEWED.equals(status)) {
            return "复核通过";
        }
        if (Fund.STATUS_DISBURSED.equals(status)) {
            return "已拨付";
        }
        return status == null ? "-" : status;
    }
}

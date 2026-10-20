package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.BusinessException;
import com.funding.common.UserContext;
import com.funding.entity.Fund;
import com.funding.entity.TimestampRecord;
import com.funding.mapper.FundMapper;
import com.funding.service.FundService;
import com.funding.service.TimestampService;
import com.funding.util.ExcelUtils;
import com.funding.util.HashUtils;
import com.funding.vo.FundVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 资金流水服务实现
 * - SPONSOR 查询一律按当前登录 userId 过滤 sponsor_id
 * - DISBURSE_ADMIN 查询时 purpose/remark 服务端置 null（申请资料锁定）
 * - 状态迁移一律条件 UPDATE，杜绝重复审核/拨付
 */
@Service
public class FundServiceImpl implements FundService {

    private static final DateTimeFormatter HASH_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private FundMapper fundMapper;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private com.funding.common.MaskingGuard maskingGuard;

    /** 大额复核阈值（元） */
    @Value("${funding.review.threshold:5000}")
    private BigDecimal reviewThreshold;

    @Override
    public IPage<FundVO> page(Integer page, Integer size, Long studentId, String keyword, String status) {
        Page<FundVO> p = new Page<>(page, size);
        Long sponsorId = null;
        if ("SPONSOR".equals(UserContext.getRole())) {
            sponsorId = UserContext.getUserId();
        }
        IPage<FundVO> result = fundMapper.selectPageWithNames(p, keyword, studentId, sponsorId, status);
        result.getRecords().forEach(this::maskByRole);
        return result;
    }

    @Override
    public FundVO getById(Long id) {
        FundVO fund = fundMapper.selectByIdWithNames(id);
        if (fund == null) {
            return null;
        }
        if ("SPONSOR".equals(UserContext.getRole())
                && (fund.getSponsorId() == null || !fund.getSponsorId().equals(UserContext.getUserId()))) {
            throw new BusinessException(403, "权限不足");
        }
        maskByRole(fund);
        return fund;
    }

    @Override
    public void saveFund(Fund fund) {
        fund.setId(null);
        // 二次脱敏校验：自由文本不得夹带未打码敏感信息
        maskingGuard.check("POST /api/funds", "用途", fund.getPurpose());
        maskingGuard.check("POST /api/funds", "备注", fund.getRemark());
        // 强制初始状态，凭证/审核/拨付字段一律清空
        fund.setStatus(Fund.STATUS_SUBMITTED);
        fund.setVoucherNo(null);
        fund.setVoucherHash(null);
        fund.setAuditBy(null);
        fund.setAuditTime(null);
        fund.setAuditRemark(null);
        fund.setDisburseBy(null);
        fund.setDisburseTime(null);
        fundMapper.insert(fund);
    }

    @Override
    public void updateFund(Fund fund) {
        // 二次脱敏校验
        maskingGuard.check("PUT /api/funds/" + fund.getId(), "用途", fund.getPurpose());
        maskingGuard.check("PUT /api/funds/" + fund.getId(), "备注", fund.getRemark());
        // 仅 SUBMITTED/REJECTED 可编辑；状态与凭证字段不允许通过本接口变更
        UpdateWrapper<Fund> uw = new UpdateWrapper<>();
        uw.eq("id", fund.getId())
                .in("status", Fund.STATUS_SUBMITTED, Fund.STATUS_REJECTED);
        Fund update = new Fund();
        update.setStudentId(fund.getStudentId());
        update.setSponsorId(fund.getSponsorId());
        update.setAmount(fund.getAmount());
        update.setFundDate(fund.getFundDate());
        update.setPurpose(fund.getPurpose());
        update.setRemark(fund.getRemark());
        // 驳回后重新编辑，回到待审核
        update.setStatus(Fund.STATUS_SUBMITTED);
        int rows = fundMapper.update(update, uw);
        if (rows == 0) {
            throw new BusinessException(400, "单据状态已变化，请刷新");
        }
    }

    @Override
    public void deleteFund(Long id) {
        // 仅 SUBMITTED 可删除（条件 DELETE）
        int rows = fundMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Fund>()
                .eq("id", id).eq("status", Fund.STATUS_SUBMITTED));
        if (rows == 0) {
            throw new BusinessException(400, "仅待审核状态的单据可删除");
        }
    }

    @Override
    public void export(HttpServletResponse response) throws Exception {
        List<FundVO> list = fundMapper.selectAllWithNames();
        List<String> headers = Arrays.asList(
                "ID", "学生", "资助人", "金额", "资助日期", "用途", "备注", "状态", "凭证号", "创建时间",
                "业务节点数", "时间戳序列", "最新存证哈希", "存证标识");
        List<List<Object>> rows = new ArrayList<>();
        for (FundVO f : list) {
            List<Object> row = new ArrayList<>();
            row.add(f.getId());
            row.add(f.getStudentName());
            row.add(f.getSponsorName());
            row.add(f.getAmount());
            row.add(f.getFundDate() != null ? f.getFundDate().toString() : "");
            row.add(f.getPurpose());
            row.add(f.getRemark());
            row.add(f.getStatus());
            row.add(f.getVoucherNo());
            row.add(f.getCreateTime() != null ? f.getCreateTime().toString() : "");
            // 可信时间戳存证信息
            List<TimestampRecord> tsList = timestampService.listByBiz("fund", f.getId());
            if (tsList.isEmpty()) {
                row.add(0);
                row.add("");
                row.add("");
                row.add("—");
            } else {
                TimestampRecord first = tsList.get(0);
                TimestampRecord last = tsList.get(tsList.size() - 1);
                row.add(tsList.size());
                row.add(tsList.size() == 1
                        ? "#" + first.getSerialNo()
                        : "#" + first.getSerialNo() + "-#" + last.getSerialNo());
                row.add(last.getTsHash() != null ? last.getTsHash().substring(0, 16) + "…" : "");
                row.add("✓ 可信存证");
            }
            rows.add(row);
        }
        ExcelUtils.writeWorkbook(response, "资金流水.xlsx", headers, rows);
    }

    @Override
    public IPage<FundVO> pendingAudit(Integer page, Integer size) {
        Page<FundVO> p = new Page<>(page, size);
        return fundMapper.selectPageWithNames(p, null, null, null, Fund.STATUS_SUBMITTED);
    }

    @Override
    public long countPendingAudit() {
        return fundMapper.countPendingAudit();
    }

    @Override
    public IPage<FundVO> pendingDisburse(Integer page, Integer size) {
        Page<FundVO> p = new Page<>(page, size);
        IPage<FundVO> result = fundMapper.selectPendingDisburse(p, reviewThreshold);
        result.getRecords().forEach(this::maskByRole);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, String result, String remark) {
        Fund fund = fundMapper.selectById(id);
        if (fund == null) {
            throw new BusinessException(404, "单据不存在");
        }
        boolean pass = "PASS".equals(result);
        // 二次脱敏校验：审核意见
        maskingGuard.check("POST /api/funds/" + id + "/audit", "审核意见", remark);
        LocalDateTime now = LocalDateTime.now();

        UpdateWrapper<Fund> uw = new UpdateWrapper<>();
        uw.eq("id", id).eq("status", Fund.STATUS_SUBMITTED);
        uw.set("audit_by", UserContext.getUserId());
        uw.set("audit_time", now);
        uw.set("audit_remark", remark);
        if (pass) {
            String voucherNo = nextVoucherNo();
            String hashSource = voucherNo + "|" + fund.getId() + "|" + fund.getStudentId()
                    + "|" + fund.getAmount() + "|" + HASH_TIME_FMT.format(now);
            uw.set("status", Fund.STATUS_AUDITED);
            uw.set("voucher_no", voucherNo);
            uw.set("voucher_hash", HashUtils.sha256(hashSource));
        } else {
            uw.set("status", Fund.STATUS_REJECTED);
        }
        int rows = fundMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException(400, "单据状态已变化，请刷新");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disburse(Long id) {
        Fund fund = fundMapper.selectById(id);
        if (fund == null) {
            throw new BusinessException(404, "单据不存在");
        }
        boolean smallAudited = Fund.STATUS_AUDITED.equals(fund.getStatus())
                && fund.getAmount().compareTo(reviewThreshold) < 0;
        boolean reviewed = Fund.STATUS_REVIEWED.equals(fund.getStatus());
        if (!smallAudited && !reviewed) {
            if (Fund.STATUS_AUDITED.equals(fund.getStatus())) {
                throw new BusinessException(400, "大额资助单需经复核通过后方可拨付");
            }
            throw new BusinessException(400, "当前状态不可拨付");
        }
        UpdateWrapper<Fund> uw = new UpdateWrapper<>();
        uw.eq("id", id).eq("status", fund.getStatus());
        uw.set("status", Fund.STATUS_DISBURSED);
        uw.set("disburse_by", UserContext.getUserId());
        uw.set("disburse_time", LocalDateTime.now());
        int rows = fundMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException(400, "单据状态已变化，请刷新");
        }
    }

    /**
     * 生成当日凭证号：Voucher-yyyyMMdd-4位序号
     * synchronized 保证单 JVM 内序号唯一，voucher_no UNIQUE 索引兜底
     */
    private synchronized String nextVoucherNo() {
        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = fundMapper.countVoucherOfDay(day) + 1;
        return String.format("Voucher-%s-%04d", day, seq);
    }

    /**
     * 字段级权限隔离：拨付管理员不可见申请资料（用途/备注）
     */
    private void maskByRole(FundVO fund) {
        if (fund == null) {
            return;
        }
        if ("DISBURSE_ADMIN".equals(UserContext.getRole())) {
            fund.setPurpose(null);
            fund.setRemark(null);
        }
    }
}

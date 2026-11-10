package com.funding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.BusinessException;
import com.funding.entity.OperationLogEntity;
import com.funding.entity.TimestampRecord;
import com.funding.vo.FundVO;
import com.funding.vo.ScoreVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** 独立投影：只返回审计必要字段，不返回原始日志参数、自由文本备注或隐私档案。 */
@Service
public class ReadOnlyAuditService {
    @Autowired private FundService fundService;
    @Autowired private ScoreService scoreService;
    @Autowired private TimestampService timestampService;
    @Autowired private OperationLogService operationLogService;

    private int page(Integer n) { return n == null ? 1 : Math.max(1, n); }
    private int size(Integer n) { return n == null ? 10 : Math.max(1, Math.min(100, n)); }

    public IPage<Map<String, Object>> funds(Integer page, Integer size) {
        return fundService.page(page(page), size(size), null, null, null).convert(this::fund);
    }

    public Map<String, Object> fund(Long id) {
        FundVO value = fundService.getById(id);
        if (value == null) throw new BusinessException(404, "资助单不存在");
        return fund(value);
    }

    private Map<String, Object> fund(FundVO f) {
        return map("id", f.getId(), "studentName", f.getStudentName(), "studentId", f.getStudentId(),
                "sponsorId", f.getSponsorId(), "amount", f.getAmount(), "fundDate", f.getFundDate(),
                "status", f.getStatus(), "voucherNo", f.getVoucherNo(), "auditTime", f.getAuditTime(),
                "disburseTime", f.getDisburseTime(), "createTime", f.getCreateTime());
    }

    public IPage<Map<String, Object>> scores(Integer page, Integer size, Long fundId) {
        return scoreService.page(page(page), size(size), null, fundId).convert(this::score);
    }

    public Map<String, Object> score(Long id) {
        ScoreVO s = scoreService.getById(id);
        if (s == null) throw new BusinessException(404, "评分不存在");
        Map<String, Object> result = score(s);
        result.put("details", s.getDetails().stream().map(d -> map("itemId", d.getItemId(),
                "itemName", d.getItemName(), "weight", d.getWeight(), "itemScore", d.getItemScore())).collect(Collectors.toList()));
        return result;
    }

    private Map<String, Object> score(ScoreVO s) {
        return map("id", s.getId(), "studentName", s.getStudentName(), "studentId", s.getStudentId(),
                "sponsorId", s.getSponsorId(), "fundId", s.getFundId(), "fundNo", s.getFundNo(),
                "scoreDate", s.getScoreDate(), "totalScore", s.getTotalScore(), "ruleVersion", s.getRuleVersion(),
                "ruleCapturedAt", s.getRuleCapturedAt());
    }

    public IPage<Map<String, Object>> timestamps(Integer page, Integer size, Long fundId) {
        return timestampService.page(page(page), size(size), fundId == null ? null : "fund", fundId, null, null)
                .convert(this::timestamp);
    }

    private Map<String, Object> timestamp(TimestampRecord t) {
        return map("serialNo", t.getSerialNo(), "bizType", t.getBizType(), "bizId", t.getBizId(),
                "nodeName", t.getNodeName(), "operatorName", t.getOperatorName(), "role", t.getRole(),
                "tsTime", t.getTsTime(), "tsHash", t.getTsHash(), "anomaly", t.getAnomaly());
    }

    public IPage<Map<String, Object>> logs(Integer page, Integer size) {
        return operationLogService.page(page(page), size(size), null, null, null).convert(this::log);
    }

    private Map<String, Object> log(OperationLogEntity l) {
        return map("id", l.getId(), "username", l.getUsername(), "role", l.getRole(), "operation", l.getOperation(),
                "targetType", l.getTargetType(), "targetId", l.getTargetId(), "createTime", l.getCreateTime());
    }

    public Map<String, Object> verify(Long fundId) {
        Map<String, Object> result = timestampService.verify(fundId == null ? null : "fund", fundId);
        return map("valid", result.get("valid"), "total", result.get("total"),
                "chainBrokenAt", result.get("chainBrokenAt"), "anomalyCount", result.get("anomalyCount"));
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }
}

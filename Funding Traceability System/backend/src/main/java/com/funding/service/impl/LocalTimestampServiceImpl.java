package com.funding.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.TimestampRecord;
import com.funding.mapper.TimestampRecordMapper;
import com.funding.service.TimestampService;
import com.funding.util.HashUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可信时间戳本地模拟实现
 * serial_no 全局递增 + SHA-256 哈希链（prev_hash → ts_hash）固化时序；
 * 写入时检测同单据时序倒序并标记预警（不阻断业务）；
 * 后续接入真实 TSA 时替换本实现，核验逻辑不变
 */
@Slf4j
@Service
public class LocalTimestampServiceImpl implements TimestampService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String GENESIS = "GENESIS";

    @Autowired
    private TimestampRecordMapper timestampRecordMapper;

    @Override
    public void stamp(String opValue, String bizType, Long bizId, Object[] args) {
        synchronized (this) {
            String[] nodeInfo = resolveNode(opValue, args);
            String node = nodeInfo[0];
            String nodeName = nodeInfo[1];
            String summary = opValue + (bizId != null ? " #" + bizId : "");

            TimestampRecord record = new TimestampRecord();
            record.setSerialNo(timestampRecordMapper.maxSerialNo() + 1);
            record.setBizType(bizType == null || bizType.isEmpty() ? "system" : bizType);
            record.setBizId(bizId);
            record.setNode(node);
            record.setNodeName(nodeName);
            record.setSummary(truncate(summary, 200));

            com.funding.common.UserInfo user = com.funding.common.UserContext.get();
            if (user != null) {
                record.setOperatorId(user.getUserId());
                record.setOperatorName(user.getUsername());
                record.setRole(user.getRole());
            }

            // 时间戳由系统底层自动生成（本地模拟 TSA 盖章时刻）
            // 截断到秒：避免 MySQL DATETIME 对毫秒四舍五入进位，导致入库时间与哈希原文不一致
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            record.setTsTime(now);

            // 时序倒序检测：同单据上一节点时间戳晚于本条则预警（仅标记不阻断）
            if (bizId != null) {
                TimestampRecord last = timestampRecordMapper.selectLast(record.getBizType(), bizId);
                if (last != null && last.getTsTime() != null && now.isBefore(last.getTsTime())) {
                    record.setAnomaly(1);
                    record.setAnomalyReason("时序异常：本节点时间早于前一节点【" + last.getNodeName()
                            + " #" + last.getSerialNo() + "】的时间戳");
                }
            }
            if (record.getAnomaly() == null) {
                record.setAnomaly(0);
            }

            // 全局哈希链：prev_hash = 链末条 ts_hash（首条 GENESIS）
            TimestampRecord lastGlobal = timestampRecordMapper.selectLastGlobal();
            String prevHash = lastGlobal == null ? GENESIS : lastGlobal.getTsHash();
            record.setPrevHash(prevHash);
            record.setTsHash(HashUtils.sha256(buildHashSource(record)));

            timestampRecordMapper.insert(record);
        }
    }

    @Override
    public void stampSeed(String node, String nodeName, String bizType, Long bizId, String summary,
                          String operatorName, String role, LocalDateTime tsTime) {
        synchronized (this) {
            TimestampRecord record = new TimestampRecord();
            record.setSerialNo(timestampRecordMapper.maxSerialNo() + 1);
            record.setBizType(bizType);
            record.setBizId(bizId);
            record.setNode(node);
            record.setNodeName(nodeName);
            record.setSummary(truncate(summary, 200));
            record.setOperatorName(operatorName);
            record.setRole(role);
            // 与 stamp 一致：截断到秒，保证入库值与哈希原文一致
            record.setTsTime(tsTime.truncatedTo(ChronoUnit.SECONDS));

            // 种子按业务时间升序补盖，正常无倒序；兜底检测同 stamp
            if (bizId != null) {
                TimestampRecord last = timestampRecordMapper.selectLast(bizType, bizId);
                if (last != null && last.getTsTime() != null && tsTime.isBefore(last.getTsTime())) {
                    record.setAnomaly(1);
                    record.setAnomalyReason("时序异常：本节点时间早于前一节点【" + last.getNodeName()
                            + " #" + last.getSerialNo() + "】的时间戳");
                }
            }
            if (record.getAnomaly() == null) {
                record.setAnomaly(0);
            }

            TimestampRecord lastGlobal = timestampRecordMapper.selectLastGlobal();
            String prevHash = lastGlobal == null ? GENESIS : lastGlobal.getTsHash();
            record.setPrevHash(prevHash);
            record.setTsHash(HashUtils.sha256(buildHashSource(record)));

            timestampRecordMapper.insert(record);
        }
    }

    /**
     * 操作描述 + 参数 → 业务节点编码/显示名
     */
    private String[] resolveNode(String opValue, Object[] args) {
        if (opValue == null) {
            return new String[]{"OTHER", "其他操作"};
        }
        switch (opValue) {
            case "新增资助申请":
                return new String[]{"SUBMIT", "提交申请"};
            case "审核资助申请":
                // 依据请求体 result 细分通过/驳回
                if (args != null) {
                    for (Object arg : args) {
                        if (arg instanceof Map) {
                            Object result = ((Map<?, ?>) arg).get("result");
                            if ("REJECT".equals(result)) {
                                return new String[]{"AUDIT_REJECT", "审核驳回"};
                            }
                            if ("PASS".equals(result)) {
                                return new String[]{"AUDIT_PASS", "审核通过"};
                            }
                        }
                    }
                }
                return new String[]{"OTHER", opValue};
            case "大额随机抽检":
                return new String[]{"REVIEW_DRAW", "大额抽检"};
            case "复核确认":
                return new String[]{"REVIEW", "复核确认"};
            case "确认拨付":
                return new String[]{"DISBURSE", "资金拨付"};
            case "修改资助申请":
                return new String[]{"VOUCHER_MODIFY", "凭证修改"};
            case "新增回访":
            case "修改回访":
                return new String[]{"VISIT_RECORD", "回访录入"};
            case "上传回访脱敏凭证":
                return new String[]{"VISIT_RECORD", "回访凭证上传"};
            case "新增学生":
            case "修改学生":
                return new String[]{"STUDENT_UPLOAD", "资料上传"};
            default:
                return new String[]{"OTHER", opValue};
        }
    }

    /**
     * 哈希原文：prev_hash|serial|biz_type|biz_id|node|ts_time|operator|summary
     * 核验重算必须保持完全一致的拼接格式
     */
    private String buildHashSource(TimestampRecord r) {
        return r.getPrevHash() + "|" + r.getSerialNo() + "|" + r.getBizType() + "|" + r.getBizId()
                + "|" + r.getNode() + "|" + TS_FMT.format(r.getTsTime()) + "|" + r.getOperatorName()
                + "|" + r.getSummary();
    }

    @Override
    public IPage<TimestampRecord> page(Integer page, Integer size, String bizType, Long bizId, String node, String keyword) {
        Page<TimestampRecord> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        return timestampRecordMapper.selectPage(p, bizType, bizId, node, keyword);
    }

    @Override
    public List<TimestampRecord> listByBiz(String bizType, Long bizId) {
        return timestampRecordMapper.selectListByBiz(bizType, bizId);
    }

    @Override
    public Map<String, Object> verify(String bizType, Long bizId) {
        List<TimestampRecord> chain = timestampRecordMapper.selectAllOrdered();
        long brokenAt = 0;
        int anomalyCount = 0;
        boolean valid = true;

        String expectedPrev = GENESIS;
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (TimestampRecord r : chain) {
            if (r.getAnomaly() != null && r.getAnomaly() == 1) {
                anomalyCount++;
            }
            boolean prevMatch = expectedPrev.equals(r.getPrevHash());
            boolean hashMatch = HashUtils.sha256(buildHashSource(r)).equals(r.getTsHash());
            if (valid && (!prevMatch || !hashMatch)) {
                valid = false;
                brokenAt = r.getSerialNo();
            }
            expectedPrev = HashUtils.sha256(buildHashSource(r));

            // 仅汇总目标单据的节点（未指定则返回空列表）
            if (bizType != null && !bizType.isEmpty() && bizType.equals(r.getBizType())
                    && (bizId == null || bizId.equals(r.getBizId()))) {
                Map<String, Object> nodeMap = new LinkedHashMap<>();
                nodeMap.put("serialNo", r.getSerialNo());
                nodeMap.put("bizType", r.getBizType());
                nodeMap.put("bizId", r.getBizId());
                nodeMap.put("node", r.getNode());
                nodeMap.put("nodeName", r.getNodeName());
                nodeMap.put("summary", r.getSummary());
                nodeMap.put("operatorName", r.getOperatorName());
                nodeMap.put("role", r.getRole());
                nodeMap.put("tsTime", r.getTsTime());
                nodeMap.put("tsHash", r.getTsHash());
                nodeMap.put("anomaly", r.getAnomaly());
                nodeMap.put("anomalyReason", r.getAnomalyReason());
                nodeMap.put("hashValid", prevMatch && hashMatch);
                nodes.add(nodeMap);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        result.put("total", chain.size());
        result.put("chainBrokenAt", brokenAt);
        result.put("anomalyCount", anomalyCount);
        result.put("nodes", nodes);
        return result;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}

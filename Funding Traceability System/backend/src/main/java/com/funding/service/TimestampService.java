package com.funding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.entity.TimestampRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 可信时间戳服务
 * 当前为本地模拟 TSA 实现（LocalTimestampServiceImpl）；
 * 后续接入真实可信时间戳服务时，仅需替换该接口的实现为调用真实 TSA 接口的版本，
 * 台账记录结构（serial_no / ts_hash / prev_hash）与核验逻辑保持不变
 */
public interface TimestampService {

    /**
     * 业务节点盖章（由 OperationLogAspect 在业务成功后调用，前端不可传入时间）
     *
     * @param opValue @OperationLog 注解的操作描述
     * @param bizType 目标类型（fund/student/visit/...）
     * @param bizId   目标业务单据 ID（可为 null，如导出类操作）
     * @param args    方法参数（用于推导审核通过/驳回等细分节点）
     */
    void stamp(String opValue, String bizType, Long bizId, Object[] args);

    /**
     * 存量数据补盖（仅 SeedTimestampRunner 空表初始化用）：
     * 显式指定盖章时间（取业务时间）与操作人，保证种子时间戳与业务时序一致
     */
    void stampSeed(String node, String nodeName, String bizType, Long bizId, String summary,
                   String operatorName, String role, LocalDateTime tsTime);

    /** 台账分页 */
    IPage<TimestampRecord> page(Integer page, Integer size, String bizType, Long bizId, String node, String keyword);

    /** 某业务单据的时间戳列表（按序号升序） */
    List<TimestampRecord> listByBiz(String bizType, Long bizId);

    /**
     * 哈希链核验：全链重算 SHA-256 比对，定位篡改点 + 汇总时序异常
     *
     * @return {valid, chainBrokenAt, anomalyCount, nodes:[{...record, hashValid}]}
     */
    Map<String, Object> verify(String bizType, Long bizId);
}

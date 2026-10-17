package com.funding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.vo.ReviewTaskVO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 大额复核抽检服务
 */
public interface ReviewTaskService {

    /**
     * 随机抽检：从大额已审核未抽检候选池中按比例抽取
     * @return {batchNo, total, drawn}
     */
    Map<String, Object> draw(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal ratio);

    /**
     * 任务分页（批次/状态过滤）
     */
    IPage<ReviewTaskVO> page(Integer page, Integer size, String batchNo, String status);

    /**
     * 复核确认：PASS → 资金单 REVIEWED；FAIL → 打回 SUBMITTED
     */
    void finish(Long id, String result, String remark);
}

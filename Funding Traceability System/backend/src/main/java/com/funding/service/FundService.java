package com.funding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.entity.Fund;
import com.funding.vo.FundVO;

import javax.servlet.http.HttpServletResponse;

/**
 * 资金流水服务
 */
public interface FundService {

    IPage<FundVO> page(Integer page, Integer size, Long studentId, String keyword, String status);

    FundVO getById(Long id);

    void saveFund(Fund fund);

    void updateFund(Fund fund);

    void deleteFund(Long id);

    void export(HttpServletResponse response) throws Exception;

    /**
     * 待审核列表（SUBMITTED）
     */
    IPage<FundVO> pendingAudit(Integer page, Integer size);

    /**
     * 待审核数量（工作台角标）
     */
    long countPendingAudit();

    /**
     * 待拨付列表（已审核小额 或 已复核）
     */
    IPage<FundVO> pendingDisburse(Integer page, Integer size);

    /**
     * 审核：result = PASS / REJECT；PASS 生成凭证号+哈希，大额自动进入复核候选
     */
    void audit(Long id, String result, String remark);

    /**
     * 拨付：前置状态 AUDITED(小额)/REVIEWED，条件 UPDATE 防并发
     */
    void disburse(Long id);
}

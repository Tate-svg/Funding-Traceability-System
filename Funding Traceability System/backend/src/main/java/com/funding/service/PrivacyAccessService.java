package com.funding.service;

import com.funding.entity.PrivacyAccessApply;

import java.util.List;
import java.util.Map;

/**
 * 隐私明文调取审批服务
 */
public interface PrivacyAccessService {

    /** 发起调取申请 */
    void apply(String virtualId, String fieldScope, String reason);

    /** 审批（两级：PENDING_L1 初审 / PENDING_L2 终审） */
    void approve(Long id, String result, String remark);

    /** 我的申请 */
    List<PrivacyAccessApply> mine();

    /** 待我审批 */
    List<PrivacyAccessApply> todo();

    /**
     * 调取隐私档案：有有效审批单返回解密明文并留痕；否则返回脱敏视图
     * 返回 Map 含 masked 标记
     */
    Map<String, Object> profile(String virtualId);
}

package com.funding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.entity.OperationLogEntity;

/**
 * 操作日志查询服务（只读）
 */
public interface OperationLogService {

    IPage<OperationLogEntity> page(Integer page, Integer size, String targetType, Long targetId, String keyword);
}

package com.funding.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.OperationLogEntity;
import com.funding.mapper.OperationLogMapper;
import com.funding.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作日志查询服务实现（只读）
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public IPage<OperationLogEntity> page(Integer page, Integer size, String targetType, Long targetId, String keyword) {
        Page<OperationLogEntity> p = new Page<>(page, size);
        return operationLogMapper.selectPage(p, targetType, targetId, keyword);
    }
}

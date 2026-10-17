package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.entity.OperationLogEntity;
import com.funding.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口（只读存证，唯一接口为查询，无任何写入口）
 */
@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    @RequirePermission("log:view")
    public Result<IPage<OperationLogEntity>> page(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  @RequestParam(required = false) String targetType,
                                                  @RequestParam(required = false) Long targetId,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(operationLogService.page(page, size, targetType, targetId, keyword));
    }
}

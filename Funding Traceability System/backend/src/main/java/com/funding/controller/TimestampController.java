package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.entity.TimestampRecord;
import com.funding.service.AuditExportService;
import com.funding.service.TimestampService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 可信时间戳台账接口（只读：仅查询与核验，无写接口）
 * 台账/时间戳数据仅 AUDIT_ADMIN（审核管理员）与 SUPERVISOR（监事）可见
 */
@RestController
@RequestMapping("/api/timestamps")
public class TimestampController {

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private AuditExportService auditExportService;

    @GetMapping
    @RequirePermission("log:view")
    public Result<IPage<TimestampRecord>> page(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @RequestParam(required = false) String bizType,
                                               @RequestParam(required = false) Long bizId,
                                               @RequestParam(required = false) String node,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(timestampService.page(page, size, bizType, bizId, node, keyword));
    }

    /**
     * 哈希链核验：全链重算比对，返回 {valid, chainBrokenAt, anomalyCount, nodes}
     */
    @GetMapping("/verify")
    @RequirePermission("log:view")
    public Result<Map<String, Object>> verify(@RequestParam(required = false) String bizType,
                                              @RequestParam(required = false) Long bizId) {
        return Result.success(timestampService.verify(bizType, bizId));
    }

    /**
     * 审计专用导出：PDF / 图片溯源单据
     * 导出件整页叠加「当前操作者账号 + 导出时间」半透明水印；
     * 普通业务导出（资金 / 学生 / 回访 / 评分 Excel）不经过此接口，不带水印。
     */
    @GetMapping("/export")
    @RequirePermission("log:view")
    @OperationLog(value = "导出审计溯源单据", type = "system")
    public void export(@RequestParam(defaultValue = "pdf") String format,
                       @RequestParam(required = false) String bizType,
                       @RequestParam(required = false) Long bizId,
                       @RequestParam(required = false) String node,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws Exception {
        auditExportService.export(response, format, bizType, bizId, node, keyword);
    }
}

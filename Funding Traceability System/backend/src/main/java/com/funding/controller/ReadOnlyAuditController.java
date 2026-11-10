package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.service.ReadOnlyAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit-readonly")
@RequirePermission("audit:readonly")
public class ReadOnlyAuditController {
    @Autowired private ReadOnlyAuditService service;

    @GetMapping("/funds")
    public Result<IPage<Map<String, Object>>> funds(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(service.funds(page, size));
    }

    @GetMapping("/funds/{id}")
    public Result<Map<String, Object>> fund(@PathVariable Long id) { return Result.success(service.fund(id)); }

    @GetMapping("/scores")
    public Result<IPage<Map<String, Object>>> scores(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) Long fundId) {
        return Result.success(service.scores(page, size, fundId));
    }

    @GetMapping("/scores/{id}")
    public Result<Map<String, Object>> score(@PathVariable Long id) { return Result.success(service.score(id)); }

    @GetMapping("/timestamps")
    public Result<IPage<Map<String, Object>>> timestamps(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size,
                                                       @RequestParam(required = false) Long fundId) {
        return Result.success(service.timestamps(page, size, fundId));
    }

    @GetMapping("/logs")
    public Result<IPage<Map<String, Object>>> logs(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(service.logs(page, size));
    }

    @GetMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestParam(required = false) Long fundId) {
        return Result.success(service.verify(fundId));
    }
}

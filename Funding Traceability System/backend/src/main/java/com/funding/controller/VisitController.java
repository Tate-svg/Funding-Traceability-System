package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.entity.Visit;
import com.funding.service.VisitService;
import com.funding.vo.VisitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * 回访接口
 */
@RestController
@RequestMapping("/api/visits")
public class VisitController {

    @Autowired
    private VisitService visitService;

    @GetMapping
    public Result<IPage<VisitVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) Long studentId,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(visitService.page(page, size, studentId, keyword));
    }

    @GetMapping("/export")
    @RequirePermission("visit:manage")
    @OperationLog(value = "导出回访记录", type = "visit")
    public void export(HttpServletResponse response) throws Exception {
        visitService.export(response);
    }

    @GetMapping("/{id}")
    public Result<VisitVO> getById(@PathVariable Long id) {
        return Result.success(visitService.getById(id));
    }

    @PostMapping
    @RequirePermission("visit:manage")
    @OperationLog(value = "新增回访", type = "visit")
    public Result<Void> save(@RequestBody Visit visit) {
        visitService.saveVisit(visit);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("visit:manage")
    @OperationLog(value = "修改回访", type = "visit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Visit visit) {
        visit.setId(id);
        visitService.updateVisit(visit);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("visit:manage")
    @OperationLog(value = "删除回访", type = "visit")
    public Result<Void> delete(@PathVariable Long id) {
        visitService.deleteVisit(id);
        return Result.success();
    }
}

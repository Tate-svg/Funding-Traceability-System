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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 回访接口
 */
@RestController
@RequestMapping("/api/visits")
public class VisitController {

    @Autowired
    private VisitService visitService;

    /**
     * 分页查询回访记录（可按学生/资助流水/关键词过滤）
     */
    @GetMapping
    public Result<IPage<VisitVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) Long studentId,
                                       @RequestParam(required = false) Long fundId,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(visitService.page(page, size, studentId, fundId, keyword));
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

    /**
     * 上传回访凭证材料（照片/访谈记录）
     * 文本类由系统自动脱敏；图片/文档类无法自动识别画面内容，需上传前人工脱敏并勾选确认
     */
    @PostMapping("/{id}/material")
    @RequirePermission("visit:manage")
    @OperationLog(value = "上传回访脱敏凭证", type = "visit")
    public Result<String> uploadMaterial(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "manuallyMasked", defaultValue = "false") boolean manuallyMasked) throws Exception {
        return Result.success(visitService.uploadMaterial(id, file, manuallyMasked));
    }

    /**
     * 下载脱敏后的回访凭证材料
     */
    @GetMapping("/{id}/material")
    public void downloadMaterial(@PathVariable Long id, HttpServletResponse response) throws Exception {
        visitService.downloadMaterial(id, response);
    }
}

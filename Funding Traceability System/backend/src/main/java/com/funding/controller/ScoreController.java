package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.service.ScoreService;
import com.funding.vo.ScoreVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * 评分接口
 */
@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @GetMapping
    public Result<IPage<ScoreVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) Long studentId) {
        return Result.success(scoreService.page(page, size, studentId));
    }

    @GetMapping("/export")
    @RequirePermission("score:manage")
    @OperationLog(value = "导出评分记录", type = "score")
    public void export(HttpServletResponse response) throws Exception {
        scoreService.export(response);
    }

    @GetMapping("/{id}")
    public Result<ScoreVO> getById(@PathVariable Long id) {
        return Result.success(scoreService.getById(id));
    }

    @PostMapping
    @RequirePermission("score:manage")
    @OperationLog(value = "新增评分", type = "score")
    public Result<Void> save(@RequestBody ScoreVO scoreVO) {
        scoreService.saveScore(scoreVO);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("score:manage")
    @OperationLog(value = "修改评分", type = "score")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScoreVO scoreVO) {
        scoreVO.setId(id);
        scoreService.updateScore(scoreVO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("score:manage")
    @OperationLog(value = "删除评分", type = "score")
    public Result<Void> delete(@PathVariable Long id) {
        scoreService.deleteScore(id);
        return Result.success();
    }
}

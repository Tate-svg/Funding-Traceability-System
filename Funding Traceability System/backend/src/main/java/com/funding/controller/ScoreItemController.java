package com.funding.controller;

import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.entity.ScoreItem;
import com.funding.service.ScoreItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评分指标接口
 */
@RestController
@RequestMapping("/api/score-items")
public class ScoreItemController {

    @Autowired
    private ScoreItemService scoreItemService;

    @GetMapping
    public Result<List<ScoreItem>> list() {
        return Result.success(scoreItemService.list());
    }

    @PostMapping
    @RequirePermission("scoreItem:manage")
    @OperationLog(value = "新增评分指标", type = "score_item")
    public Result<Void> save(@RequestBody ScoreItem item) {
        scoreItemService.save(item);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("scoreItem:manage")
    @OperationLog(value = "修改评分指标", type = "score_item")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScoreItem item) {
        item.setId(id);
        scoreItemService.update(item);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("scoreItem:manage")
    @OperationLog(value = "删除评分指标", type = "score_item")
    public Result<Void> delete(@PathVariable Long id) {
        scoreItemService.delete(id);
        return Result.success();
    }
}

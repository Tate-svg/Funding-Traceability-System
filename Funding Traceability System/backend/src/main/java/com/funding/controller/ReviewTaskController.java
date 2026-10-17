package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.service.ReviewTaskService;
import com.funding.vo.ReviewTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 大额复核抽检接口（监事/审计）
 */
@RestController
@RequestMapping("/api/review-tasks")
public class ReviewTaskController {

    @Autowired
    private ReviewTaskService reviewTaskService;

    /**
     * 随机抽检：{minAmount, maxAmount, ratio} → {batchNo, total, drawn}
     */
    @PostMapping("/draw")
    @RequirePermission("review:draw")
    @OperationLog(value = "大额随机抽检", type = "review_task")
    public Result<Map<String, Object>> draw(@RequestBody Map<String, BigDecimal> body) {
        return Result.success(reviewTaskService.draw(
                body.get("minAmount"), body.get("maxAmount"), body.get("ratio")));
    }

    @GetMapping
    @RequirePermission("review:finish")
    public Result<IPage<ReviewTaskVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String batchNo,
                                            @RequestParam(required = false) String status) {
        return Result.success(reviewTaskService.page(page, size, batchNo, status));
    }

    /**
     * 复核确认：{result:"PASS"/"FAIL", remark}
     */
    @PostMapping("/{id}/finish")
    @RequirePermission("review:finish")
    @OperationLog(value = "复核确认", type = "review_task")
    public Result<Void> finish(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewTaskService.finish(id, body.get("result"), body.get("remark"));
        return Result.success();
    }
}

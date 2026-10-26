package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.entity.Fund;
import com.funding.service.FundService;
import com.funding.service.FundVerifyService;
import com.funding.vo.FundQrCodeVO;
import com.funding.vo.FundVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 资金流水接口
 * 状态机：SUBMITTED → AUDITED(驳回 REJECTED) → REVIEWED → DISBURSED
 */
@RestController
@RequestMapping("/api/funds")
public class FundController {

    @Autowired
    private FundService fundService;

    @Autowired
    private FundVerifyService fundVerifyService;

    @GetMapping
    public Result<IPage<FundVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size,
                                      @RequestParam(required = false) Long studentId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status) {
        return Result.success(fundService.page(page, size, studentId, keyword, status));
    }

    @GetMapping("/pending-audit")
    @RequirePermission("fund:audit")
    public Result<IPage<FundVO>> pendingAudit(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(fundService.pendingAudit(page, size));
    }

    @GetMapping("/pending-audit/count")
    @RequirePermission("fund:audit")
    public Result<Long> countPendingAudit() {
        return Result.success(fundService.countPendingAudit());
    }

    @GetMapping("/pending-disburse")
    @RequirePermission("fund:disburse")
    public Result<IPage<FundVO>> pendingDisburse(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(fundService.pendingDisburse(page, size));
    }

    @GetMapping("/export")
    @RequirePermission("fund:export")
    @OperationLog(value = "导出资金流水", type = "fund")
    public void export(HttpServletResponse response) throws Exception {
        fundService.export(response);
    }

    @GetMapping("/{id}")
    public Result<FundVO> getById(@PathVariable Long id) {
        return Result.success(fundService.getById(id));
    }

    /**
     * 生成资助凭证溯源核验二维码（内部入口，需登录 + fund:view）
     * 二维码内容为公开核验页地址，扫码不需要登录
     */
    @GetMapping("/{id}/qrcode")
    @RequirePermission("fund:view")
    public Result<FundQrCodeVO> qrcode(@PathVariable Long id) {
        return Result.success(fundVerifyService.qrCode(id));
    }

    @PostMapping
    @RequirePermission("fund:create")
    @OperationLog(value = "新增资助申请", type = "fund")
    public Result<Void> save(@RequestBody Fund fund) {
        fundService.saveFund(fund);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("fund:update")
    @OperationLog(value = "修改资助申请", type = "fund")
    public Result<Void> update(@PathVariable Long id, @RequestBody Fund fund) {
        fund.setId(id);
        fundService.updateFund(fund);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("fund:delete")
    @OperationLog(value = "删除资助申请", type = "fund")
    public Result<Void> delete(@PathVariable Long id) {
        fundService.deleteFund(id);
        return Result.success();
    }

    /**
     * 审核：{result:"PASS"/"REJECT", remark:"..."}
     */
    @PostMapping("/{id}/audit")
    @RequirePermission("fund:audit")
    @OperationLog(value = "审核资助申请", type = "fund")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        fundService.audit(id, body.get("result"), body.get("remark"));
        return Result.success();
    }

    /**
     * 确认拨付
     */
    @PostMapping("/{id}/disburse")
    @RequirePermission("fund:disburse")
    @OperationLog(value = "确认拨付", type = "fund")
    public Result<Void> disburse(@PathVariable Long id) {
        fundService.disburse(id);
        return Result.success();
    }
}

package com.funding.controller;

import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.entity.PrivacyAccessApply;
import com.funding.service.PrivacyAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 隐私明文调取审批接口
 * 普通岗位默认只见脱敏数据；明文调取走两级审批（审核管理员初审 -> 监事终审），
 * 通过后限时可见，调取全程留痕
 */
@RestController
@RequestMapping("/api/privacy")
public class PrivacyAccessController {

    @Autowired
    private PrivacyAccessService privacyAccessService;

    /**
     * 发起调取申请：{virtualId, fieldScope, reason}
     */
    @PostMapping("/access/apply")
    @RequirePermission("privacy:apply")
    @OperationLog(value = "申请隐私调取", type = "privacy_access")
    public Result<Void> apply(@RequestBody Map<String, String> body) {
        privacyAccessService.apply(body.get("virtualId"), body.get("fieldScope"), body.get("reason"));
        return Result.success();
    }

    /**
     * 审批：{result:"APPROVE"/"REJECT", remark:"..."}
     * 初审需 privacy:approve:l1，终审需 privacy:approve:l2（服务内按当前状态校验级别）
     */
    @PostMapping("/access/{id}/approve")
    @RequirePermission({"privacy:approve:l1", "privacy:approve:l2"})
    @OperationLog(value = "审批隐私调取", type = "privacy_access")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        privacyAccessService.approve(id, body.get("result"), body.get("remark"));
        return Result.success();
    }

    /**
     * 我的申请列表
     */
    @GetMapping("/access/mine")
    public Result<List<PrivacyAccessApply>> mine() {
        return Result.success(privacyAccessService.mine());
    }

    /**
     * 待我审批列表（按角色分流初审/终审）
     */
    @GetMapping("/access/todo")
    public Result<List<PrivacyAccessApply>> todo() {
        return Result.success(privacyAccessService.todo());
    }

    /**
     * 调取隐私档案：有有效审批单返回解密明文并留痕，否则返回脱敏视图
     */
    @GetMapping("/profile/{virtualId}")
    public Result<Map<String, Object>> profile(@PathVariable String virtualId) {
        return Result.success(privacyAccessService.profile(virtualId));
    }
}

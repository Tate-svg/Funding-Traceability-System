package com.funding.controller;

import com.funding.common.Result;
import com.funding.service.AdminPrivacyConfigService;
import com.funding.service.SensitiveViewSessionService;
import com.funding.common.SensitiveMaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/privacy/mask")
public class PrivacyMaskController {

    @Autowired
    private AdminPrivacyConfigService configService;

    @Autowired
    private SensitiveViewSessionService sessionService;

    @Autowired
    private SensitiveMaskUtil maskUtil;

    //1. 获取管理员当前二次遮挡状态
    @GetMapping("/getMaskStatus")
    public Result<Map<String,Object>> getMaskStatus(@RequestParam Long adminId){
        Boolean status = configService.getMaskStatus(adminId);
        Map<String,Object> map = new HashMap<>();
        map.put("isSecondMask", status);
        return Result.success(map);
    }

    //2. 切换二次遮挡开关
    @PostMapping("/switchMask")
    public Result<String> switchMask(@RequestParam Long adminId, @RequestParam Boolean isSecondMask){
        configService.switchMask(adminId, isSecondMask);
        return Result.success("切换成功");
    }

    //3. 创建脱敏查看会话（打开弹窗调用）
    @PostMapping("/createViewSession")
    public Result<Map<String,Object>> createViewSession(@RequestParam Long adminId, @RequestParam Long targetId){
        String sessionKey = sessionService.createSession(adminId, targetId);
        Map<String,Object> map = new HashMap<>();
        map.put("sessionKey", sessionKey);
        return Result.success(map);
    }

    //4. 获取脱敏详情（核心！先校验session，再根据开关是否脱敏）
    @GetMapping("/getSensitiveDetail")
    public Result<?> getSensitiveDetail(@RequestParam String sessionKey,
                                        @RequestParam Long adminId,
                                        @RequestParam Long targetId){
        // 第一步校验会话是否过期
        boolean valid = sessionService.checkSessionValid(sessionKey);
        if (!valid){
            return Result.error("会话已超时，敏感信息不可查看");
        }
        //拿到二次遮挡开关状态
        Boolean secondMask = configService.getMaskStatus(adminId);

        // ========== 这里后面替换成你自己查询原始数据业务代码 ==========
        // OriginalData originalData = getRawData(targetId);
        // String bankCard = maskUtil.maskBankCard(originalData.getBankCard(), secondMask);
        // String address = maskUtil.maskAddress(originalData.getAddress(), secondMask);
        // Map<String,Object> res = new HashMap<>();
        // res.put("bankCard",bankCard);
        // res.put("address",address);
        // return Result.success(res);
        // ==========================================================

        return Result.success();
    }

    //5. 手动关闭会话
    @PostMapping("/closeSession")
    public Result<String> closeSession(@RequestParam String sessionKey){
        sessionService.closeSession(sessionKey);
        return Result.success("会话已关闭");
    }
}
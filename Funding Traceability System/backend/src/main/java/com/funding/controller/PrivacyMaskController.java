package com.funding.controller;

import com.funding.common.Result;
import com.funding.common.SensitiveMaskUtil;
import com.funding.common.UserContext;
import com.funding.common.UserInfo;
import com.funding.entity.PrivacyAccessApply;
import com.funding.mapper.PrivacyAccessApplyMapper;
import com.funding.service.AdminPrivacyConfigService;
import com.funding.service.PrivacyAccessService;
import com.funding.service.SensitiveViewSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 敏感字段二次遮挡 + 查看会话（超时自动关闭）
 * adminId 优先取当前登录用户，避免前端伪造他人配置
 */
@RestController
@RequestMapping("/api/privacy/mask")
public class PrivacyMaskController {

    @Autowired
    private AdminPrivacyConfigService configService;

    @Autowired
    private SensitiveViewSessionService sessionService;

    @Autowired
    private SensitiveMaskUtil maskUtil;

    @Autowired
    private PrivacyAccessApplyMapper applyMapper;

    @Autowired
    private PrivacyAccessService privacyAccessService;

    //1. 获取管理员当前二次遮挡状态
    @GetMapping("/getMaskStatus")
    public Result<Map<String, Object>> getMaskStatus(@RequestParam(required = false) Long adminId) {
        Long current = currentAdminId(adminId);
        if (current == null) {
            return Result.unauthorized();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("isSecondMask", configService.getMaskStatus(current));
        return Result.success(map);
    }

    //2. 切换二次遮挡开关
    @PostMapping("/switchMask")
    public Result<String> switchMask(@RequestParam(required = false) Long adminId,
                                     @RequestParam Boolean isSecondMask) {
        Long current = currentAdminId(adminId);
        if (current == null) {
            return Result.unauthorized();
        }
        configService.switchMask(current, isSecondMask);
        return Result.success("切换成功");
    }

    //3. 创建脱敏查看会话（打开弹窗调用）
    @PostMapping("/createViewSession")
    public Result<Map<String, Object>> createViewSession(@RequestParam(required = false) Long adminId,
                                                         @RequestParam Long targetId) {
        Long current = currentAdminId(adminId);
        if (current == null) {
            return Result.unauthorized();
        }
        String sessionKey = sessionService.createSession(current, targetId);
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", sessionKey);
        map.put("expireMinutes", sessionService.getExpireMin());
        return Result.success(map);
    }

    //4. 获取敏感详情：先校验会话，再按审批授权范围取数并留痕，最后套用二次遮挡
    @GetMapping("/getSensitiveDetail")
    public Result<Map<String, Object>> getSensitiveDetail(@RequestParam String sessionKey,
                                                          @RequestParam(required = false) Long adminId,
                                                          @RequestParam Long targetId) {
        if (!sessionService.checkSessionValid(sessionKey)) {
            return Result.error(401, "会话已超时，敏感信息不可查看");
        }
        Long current = currentAdminId(adminId);
        if (current == null) {
            return Result.unauthorized();
        }
        PrivacyAccessApply apply = applyMapper.selectById(targetId);
        if (apply == null) {
            return Result.error(404, "未找到对应的调取申请单");
        }
        // 复用既有隐私调取服务：无有效审批时返回脱敏视图，有授权时返回明文并写调取日志
        Map<String, Object> profile = privacyAccessService.profile(apply.getVirtualId());

        Boolean secondMask = configService.getMaskStatus(current);
        Map<String, Object> data = new LinkedHashMap<>(profile);
        if (Boolean.TRUE.equals(secondMask)) {
            Object phone = data.get("phone");
            if (phone instanceof String) {
                data.put("phone", maskUtil.maskPhone((String) phone, true));
            }
            Object address = data.get("familyAddress");
            if (address instanceof String) {
                data.put("familyAddress", maskUtil.maskAddress((String) address, true));
            }
        }
        data.put("secondMask", secondMask);
        data.put("applyId", apply.getId());
        data.put("virtualId", apply.getVirtualId());
        data.put("expireMinutes", sessionService.getExpireMin());
        return Result.success(data);
    }

    //5. 手动关闭会话
    @PostMapping("/closeSession")
    public Result<String> closeSession(@RequestParam String sessionKey) {
        sessionService.closeSession(sessionKey);
        return Result.success("会话已关闭");
    }

    /** 当前管理员id：优先取登录态，其次回退到入参 */
    private Long currentAdminId(Long adminId) {
        UserInfo user = UserContext.get();
        if (user != null && user.getUserId() != null) {
            return user.getUserId();
        }
        return adminId;
    }
}

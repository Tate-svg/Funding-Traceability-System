package com.funding.common;

import com.funding.entity.MaskViolationLog;
import com.funding.mapper.MaskViolationLogMapper;
import com.funding.util.MaskingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 二次脱敏校验（系统上传后自动二次脱敏的拦截层）
 * 业务写入前调用：命中未打码敏感内容即写拦截留痕并 400 拒绝提交
 */
@Component
public class MaskingGuard {

    @Autowired
    private MaskViolationLogMapper maskViolationLogMapper;

    /**
     * 校验文本字段；命中未打码敏感内容则记录 mask_violation_log 并抛 400
     *
     * @param api   来源接口，如 POST /api/funds
     * @param field 字段中文名，如 备注
     * @param text  待校验文本（空值直接放行）
     */
    public void check(String api, String field, String text) {
        List<MaskingEngine.Hit> hits = MaskingEngine.scan(text);
        if (hits.isEmpty()) {
            return;
        }
        MaskingEngine.Hit first = hits.get(0);
        MaskViolationLog log = new MaskViolationLog();
        UserInfo user = UserContext.get();
        if (user != null) {
            log.setUserId(user.getUserId());
            log.setUsername(user.getUsername());
        }
        log.setApi(api);
        log.setField(field);
        log.setRule(first.getRule());
        // 留痕只存打码后片段，避免明文回流业务库
        log.setHitText(first.getMasked());
        maskViolationLogMapper.insert(log);
        throw new BusinessException(400, "【" + field + "】含未脱敏" + ruleName(first.getRule())
                + "（" + first.getMasked() + "），请先打码后重新提交");
    }

    private String ruleName(String rule) {
        switch (rule) {
            case "ID_CARD":
                return "身份证号";
            case "PHONE":
                return "手机号";
            case "BANK_CARD":
                return "银行卡号";
            default:
                return "长串数字";
        }
    }
}

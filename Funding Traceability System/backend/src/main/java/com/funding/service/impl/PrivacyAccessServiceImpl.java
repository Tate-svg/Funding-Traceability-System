package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.funding.common.BusinessException;
import com.funding.common.PermissionHolder;
import com.funding.common.UserContext;
import com.funding.common.UserInfo;
import com.funding.entity.PrivacyAccessApply;
import com.funding.entity.PrivacyAccessLog;
import com.funding.entity.PrivacyProfile;
import com.funding.entity.Student;
import com.funding.mapper.PrivacyAccessApplyMapper;
import com.funding.mapper.PrivacyAccessLogMapper;
import com.funding.mapper.StudentMapper;
import com.funding.privacymapper.PrivacyProfileMapper;
import com.funding.service.PrivacyAccessService;
import com.funding.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 隐私明文调取审批实现
 * 状态机：PENDING_L1 -(初审通过)-> PENDING_L2 -(终审通过)-> APPROVED(限时2h)
 *         任一级驳回 -> REJECTED
 * 明文调取必须持有本人 APPROVED 且未过期的申请单，每次调取写 privacy_access_log
 */
@Service
public class PrivacyAccessServiceImpl implements PrivacyAccessService {

    /** 可调取字段全集 */
    private static final List<String> ALL_FIELDS =
            Arrays.asList("name", "idCard", "phone", "familyAddress", "familySituation");

    /** 终审通过后明文可见时长（小时） */
    private static final int APPROVE_VALID_HOURS = 2;

    @Autowired
    private PrivacyAccessApplyMapper applyMapper;

    @Autowired
    private PrivacyAccessLogMapper accessLogMapper;

    @Autowired
    private PrivacyProfileMapper privacyProfileMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private PermissionHolder permissionHolder;

    @Override
    public void apply(String virtualId, String fieldScope, String reason) {
        if (virtualId == null || virtualId.isEmpty()) {
            throw new BusinessException(400, "请选择调取对象");
        }
        Student student = studentMapper.selectOne(
                new QueryWrapper<Student>().eq("virtual_id", virtualId));
        if (student == null) {
            throw new BusinessException(404, "虚拟ID对应的学生不存在");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(400, "请填写调取理由");
        }
        String scope = normalizeScope(fieldScope);
        UserInfo user = currentUser();

        PrivacyAccessApply apply = new PrivacyAccessApply();
        apply.setApplicantId(user.getUserId());
        apply.setApplicantName(user.getUsername());
        apply.setVirtualId(virtualId);
        apply.setFieldScope(scope);
        apply.setReason(reason.trim());
        apply.setStatus(PrivacyAccessApply.STATUS_PENDING_L1);
        applyMapper.insert(apply);
    }

    @Override
    public void approve(Long id, String result, String remark) {
        PrivacyAccessApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        boolean approve = "APPROVE".equals(result);
        if (!approve && !"REJECT".equals(result)) {
            throw new BusinessException(400, "result 只能为 APPROVE/REJECT");
        }
        UserInfo user = currentUser();
        LocalDateTime now = LocalDateTime.now();
        String status = apply.getStatus();

        UpdateWrapper<PrivacyAccessApply> uw = new UpdateWrapper<>();
        uw.eq("id", id);

        if (PrivacyAccessApply.STATUS_PENDING_L1.equals(status)) {
            // 初审：须持有 privacy:approve:l1（审核管理员）
            if (!permissionHolder.hasPermission(user.getRole(), new String[]{"privacy:approve:l1"})) {
                throw new BusinessException(403, "当前角色无初审权限");
            }
            uw.eq("status", PrivacyAccessApply.STATUS_PENDING_L1);
            uw.set("l1_by", user.getUserId());
            uw.set("l1_time", now);
            uw.set("l1_remark", remark);
            uw.set("status", approve ? PrivacyAccessApply.STATUS_PENDING_L2 : PrivacyAccessApply.STATUS_REJECTED);
        } else if (PrivacyAccessApply.STATUS_PENDING_L2.equals(status)) {
            // 终审：须持有 privacy:approve:l2（监事）
            if (!permissionHolder.hasPermission(user.getRole(), new String[]{"privacy:approve:l2"})) {
                throw new BusinessException(403, "当前角色无终审权限");
            }
            uw.eq("status", PrivacyAccessApply.STATUS_PENDING_L2);
            uw.set("l2_by", user.getUserId());
            uw.set("l2_time", now);
            uw.set("l2_remark", remark);
            if (approve) {
                uw.set("status", PrivacyAccessApply.STATUS_APPROVED);
                uw.set("expire_time", now.plusHours(APPROVE_VALID_HOURS));
            } else {
                uw.set("status", PrivacyAccessApply.STATUS_REJECTED);
            }
        } else {
            throw new BusinessException(400, "该审批单已办结，请刷新");
        }

        int rows = applyMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException(400, "审批单状态已变化，请刷新");
        }
    }

    @Override
    public List<PrivacyAccessApply> mine() {
        return applyMapper.selectList(new QueryWrapper<PrivacyAccessApply>()
                .eq("applicant_id", currentUser().getUserId())
                .orderByDesc("id"));
    }

    @Override
    public List<PrivacyAccessApply> todo() {
        String role = currentUser().getRole();
        QueryWrapper<PrivacyAccessApply> qw = new QueryWrapper<>();
        if (permissionHolder.hasPermission(role, new String[]{"privacy:approve:l1"})) {
            qw.eq("status", PrivacyAccessApply.STATUS_PENDING_L1);
        } else if (permissionHolder.hasPermission(role, new String[]{"privacy:approve:l2"})) {
            qw.eq("status", PrivacyAccessApply.STATUS_PENDING_L2);
        } else {
            return java.util.Collections.emptyList();
        }
        qw.orderByAsc("id");
        return applyMapper.selectList(qw);
    }

    @Override
    public Map<String, Object> profile(String virtualId) {
        Student student = studentMapper.selectOne(
                new QueryWrapper<Student>().eq("virtual_id", virtualId));
        if (student == null) {
            throw new BusinessException(404, "虚拟ID对应的学生不存在");
        }
        UserInfo user = currentUser();

        // 查本人 APPROVED 且未过期的最新申请单
        PrivacyAccessApply valid = applyMapper.selectOne(new QueryWrapper<PrivacyAccessApply>()
                .eq("applicant_id", user.getUserId())
                .eq("virtual_id", virtualId)
                .eq("status", PrivacyAccessApply.STATUS_APPROVED)
                .gt("expire_time", LocalDateTime.now())
                .orderByDesc("id")
                .last("LIMIT 1"));

        Map<String, Object> result = new HashMap<>();
        if (valid == null) {
            // 无有效审批：仅返回业务库脱敏视图
            result.put("masked", true);
            result.put("virtualId", student.getVirtualId());
            result.put("maskName", student.getMaskName());
            result.put("school", student.getSchool());
            result.put("grade", student.getGrade());
            result.put("gender", student.getGender());
            return result;
        }

        // 有有效审批：按申请范围解密返回，并全程留痕
        PrivacyProfile profile = privacyProfileMapper.selectOne(
                new QueryWrapper<PrivacyProfile>().eq("virtual_id", virtualId));
        Map<String, Object> plain = new LinkedHashMap<>();
        List<String> fields = Arrays.asList(valid.getFieldScope().split(","));
        if (profile != null) {
            for (String f : fields) {
                switch (f) {
                    case "name":
                        plain.put("name", CryptoUtils.decrypt(profile.getNameEnc()));
                        break;
                    case "idCard":
                        plain.put("idCard", CryptoUtils.decrypt(profile.getIdCardEnc()));
                        break;
                    case "phone":
                        plain.put("phone", CryptoUtils.decrypt(profile.getPhoneEnc()));
                        break;
                    case "familyAddress":
                        plain.put("familyAddress", CryptoUtils.decrypt(profile.getFamilyAddressEnc()));
                        break;
                    case "familySituation":
                        plain.put("familySituation", CryptoUtils.decrypt(profile.getFamilySituationEnc()));
                        break;
                    default:
                        break;
                }
            }
        }

        // 调取留痕：谁/何时/哪生/哪些字段/关联申请单
        PrivacyAccessLog log = new PrivacyAccessLog();
        log.setApplyId(valid.getId());
        log.setUserId(user.getUserId());
        log.setUsername(user.getUsername());
        log.setVirtualId(virtualId);
        log.setFields(String.join(",", plain.keySet()));
        accessLogMapper.insert(log);

        result.put("masked", false);
        result.put("virtualId", virtualId);
        result.put("applyId", valid.getId());
        result.put("expireTime", valid.getExpireTime());
        result.putAll(plain);
        return result;
    }

    /** 规范化字段范围：非法字段剔除，为空则默认全集 */
    private String normalizeScope(String fieldScope) {
        if (fieldScope == null || fieldScope.trim().isEmpty()) {
            return String.join(",", ALL_FIELDS);
        }
        StringBuilder sb = new StringBuilder();
        for (String f : fieldScope.split(",")) {
            String t = f.trim();
            if (ALL_FIELDS.contains(t)) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(t);
            }
        }
        if (sb.length() == 0) {
            throw new BusinessException(400, "调取字段范围不合法");
        }
        return sb.toString();
    }

    private UserInfo currentUser() {
        UserInfo user = UserContext.get();
        if (user == null) {
            throw new BusinessException(401, "未登录");
        }
        return user;
    }
}

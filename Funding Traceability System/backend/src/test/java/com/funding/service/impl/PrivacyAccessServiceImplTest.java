package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.funding.common.BusinessException;
import com.funding.common.PermissionHolder;
import com.funding.common.UserContext;
import com.funding.common.UserInfo;
import com.funding.entity.PrivacyAccessApply;
import com.funding.entity.PrivacyAccessLog;
import com.funding.entity.PrivacyProfile;
import com.funding.entity.SensitiveViewSession;
import com.funding.entity.Student;
import com.funding.entity.SysUser;
import com.funding.mapper.PrivacyAccessApplyMapper;
import com.funding.mapper.PrivacyAccessLogMapper;
import com.funding.mapper.StudentMapper;
import com.funding.mapper.SysUserMapper;
import com.funding.privacymapper.PrivacyProfileMapper;
import com.funding.util.CryptoUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 隐私明文调取业务单元测试
 * 全部依赖使用 Mockito 模拟，不连接任何数据库，不污染业务库
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrivacyAccessServiceImplTest {

    private static final String VIRTUAL_ID = "STU-a1b2c3d4e5f6";

    @Mock
    private PrivacyAccessApplyMapper applyMapper;

    @Mock
    private PrivacyAccessLogMapper accessLogMapper;

    @Mock
    private PrivacyProfileMapper privacyProfileMapper;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private PermissionHolder permissionHolder;

    @InjectMocks
    private PrivacyAccessServiceImpl service;

    @BeforeAll
    static void initCryptoKey() {
        CryptoUtils cryptoUtils = new CryptoUtils();
        ReflectionTestUtils.setField(cryptoUtils, "aesKey", "FundingPrivacy16");
        cryptoUtils.init();
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    private Student student() {
        Student student = new Student();
        student.setId(1L);
        student.setVirtualId(VIRTUAL_ID);
        student.setMaskName("李**");
        student.setSchool("北京第一中学");
        student.setGrade("高二");
        student.setGender("男");
        return student;
    }

    private PrivacyAccessApply apply(String status, Long applicantId, String fieldScope, LocalDateTime expire) {
        PrivacyAccessApply apply = new PrivacyAccessApply();
        apply.setId(1L);
        apply.setApplicantId(applicantId);
        apply.setApplicantName("sponsor");
        apply.setVirtualId(VIRTUAL_ID);
        apply.setFieldScope(fieldScope);
        apply.setStatus(status);
        apply.setExpireTime(expire);
        return apply;
    }

    // ==================== 申请提交 ====================

    @Test
    @DisplayName("申请提交成功：落库为待初审，申请人取自登录态")
    void applyShouldCreatePendingL1() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        when(studentMapper.selectOne(any())).thenReturn(student());
        when(applyMapper.insert(any())).thenReturn(1);

        service.apply(VIRTUAL_ID, "name,phone", "核验资助对象联系方式");

        ArgumentCaptor<PrivacyAccessApply> captor = ArgumentCaptor.forClass(PrivacyAccessApply.class);
        verify(applyMapper).insert(captor.capture());
        PrivacyAccessApply saved = captor.getValue();
        assertEquals(PrivacyAccessApply.STATUS_PENDING_L1, saved.getStatus());
        assertEquals(2L, saved.getApplicantId());
        assertEquals("name,phone", saved.getFieldScope());
        assertEquals("核验资助对象联系方式", saved.getReason());
    }

    @Test
    @DisplayName("未填写调取理由时拒绝提交")
    void applyShouldRejectEmptyReason() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        when(studentMapper.selectOne(any())).thenReturn(student());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(VIRTUAL_ID, "name", "  "));
        assertEquals(400, ex.getCode());
        verify(applyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("虚拟ID不存在时返回 404")
    void applyShouldRejectUnknownVirtualId() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        when(studentMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply("STU-not-exist", "name", "理由"));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("字段范围含非法字段时拒绝提交")
    void applyShouldRejectIllegalFieldScope() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        when(studentMapper.selectOne(any())).thenReturn(student());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(VIRTUAL_ID, "password", "理由"));
        assertEquals(400, ex.getCode());
    }

    // ==================== 两级审批 ====================

    @Test
    @DisplayName("初审通过：状态推进到待终审")
    void l1ApproveShouldMoveToPendingL2() {
        UserContext.set(new UserInfo(1L, "audit_admin", "AUDIT_ADMIN"));
        when(applyMapper.selectById(1L)).thenReturn(apply(
                PrivacyAccessApply.STATUS_PENDING_L1, 2L, "name,phone", null));
        when(permissionHolder.hasPermission(eq("AUDIT_ADMIN"), any(String[].class))).thenReturn(true);
        when(applyMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.approve(1L, "APPROVE", "初审通过");

        UpdateWrapper<?> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(PrivacyAccessApply.STATUS_PENDING_L2));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(1L));
    }

    @Test
    @DisplayName("无初审权限的角色不能初审")
    void l1ApproveShouldRejectWrongRole() {
        UserContext.set(new UserInfo(6L, "supervisor2", "SUPERVISOR"));
        when(applyMapper.selectById(1L)).thenReturn(apply(
                PrivacyAccessApply.STATUS_PENDING_L1, 2L, "name", null));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(1L, "APPROVE", "越权初审"));
        assertEquals(403, ex.getCode());
        verify(applyMapper, never()).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("不得审批本人提交的申请（双人审批约束）")
    void approveShouldRejectSelfApproval() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        when(applyMapper.selectById(1L)).thenReturn(apply(
                PrivacyAccessApply.STATUS_PENDING_L1, 2L, "name", null));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(1L, "APPROVE", "自审"));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("终审通过：状态置为 APPROVED 并写入限时有效期")
    void l2ApproveShouldSetApprovedAndExpireTime() {
        UserContext.set(new UserInfo(6L, "supervisor2", "SUPERVISOR"));
        when(applyMapper.selectById(1L)).thenReturn(apply(
                PrivacyAccessApply.STATUS_PENDING_L2, 2L, "name,phone", null));
        when(permissionHolder.hasPermission(eq("SUPERVISOR"), any(String[].class))).thenReturn(true);
        when(applyMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.approve(1L, "APPROVE", "终审通过");

        UpdateWrapper<?> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(PrivacyAccessApply.STATUS_APPROVED));
        assertTrue(wrapper.getParamNameValuePairs().values().stream()
                .anyMatch(v -> v instanceof LocalDateTime));
    }

    @Test
    @DisplayName("终审驳回：状态置为 REJECTED")
    void l2RejectShouldSetRejected() {
        UserContext.set(new UserInfo(6L, "supervisor2", "SUPERVISOR"));
        when(applyMapper.selectById(1L)).thenReturn(apply(
                PrivacyAccessApply.STATUS_PENDING_L2, 2L, "name", null));
        when(permissionHolder.hasPermission(eq("SUPERVISOR"), any(String[].class))).thenReturn(true);
        when(applyMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.approve(1L, "REJECT", "材料不足");

        UpdateWrapper<?> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(PrivacyAccessApply.STATUS_REJECTED));
    }

    @Test
    @DisplayName("已办结申请单不能重复审批")
    void approveShouldRejectClosedApply() {
        UserContext.set(new UserInfo(6L, "supervisor2", "SUPERVISOR"));
        when(applyMapper.selectById(1L)).thenReturn(apply(
                PrivacyAccessApply.STATUS_APPROVED, 2L, "name", LocalDateTime.now().plusHours(1)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(1L, "APPROVE", "重复审批"));
        assertEquals(400, ex.getCode());
    }

    // ==================== 待办分流 ====================

    @Test
    @DisplayName("初审角色待办只看待初审")
    void todoShouldReturnPendingL1ForAuditRole() {
        UserContext.set(new UserInfo(1L, "audit_admin", "AUDIT_ADMIN"));
        when(permissionHolder.hasPermission(eq("AUDIT_ADMIN"), any(String[].class))).thenReturn(true);
        List<PrivacyAccessApply> pending = Collections.singletonList(
                apply(PrivacyAccessApply.STATUS_PENDING_L1, 2L, "name", null));
        when(applyMapper.selectList(any())).thenReturn(pending);

        assertEquals(1, service.todo().size());
    }

    @Test
    @DisplayName("无审批权限角色待办为空，不查询数据库")
    void todoShouldBeEmptyForSponsor() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));

        assertTrue(service.todo().isEmpty());
        verify(applyMapper, never()).selectList(any());
    }

    // ==================== 明文调取 / 脱敏视图 ====================

    @Test
    @DisplayName("无有效审批单时只返回脱敏视图，且不写调取日志")
    void profileWithoutApprovalShouldBeMasked() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        when(studentMapper.selectOne(any())).thenReturn(student());
        when(applyMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.profile(VIRTUAL_ID);

        assertEquals(true, result.get("masked"));
        assertEquals("李**", result.get("maskName"));
        assertNull(result.get("phone"));
        assertNull(result.get("idCard"));
        verify(accessLogMapper, never()).insert(any());
    }

    @Test
    @DisplayName("持有效终审通过单据时返回明文并写入调取留痕")
    void profileWithApprovalShouldReturnPlaintextAndLog() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        when(studentMapper.selectOne(any())).thenReturn(student());
        when(applyMapper.selectOne(any())).thenReturn(apply(
                PrivacyAccessApply.STATUS_APPROVED, 2L, "name,phone",
                LocalDateTime.now().plusHours(1)));
        when(privacyProfileMapper.selectOne(any())).thenReturn(profile());

        Map<String, Object> result = service.profile(VIRTUAL_ID);

        assertEquals(false, result.get("masked"));
        assertEquals("张三", result.get("name"));
        assertEquals("13812340002", result.get("phone"));

        ArgumentCaptor<PrivacyAccessLog> logCaptor = ArgumentCaptor.forClass(PrivacyAccessLog.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals(1L, logCaptor.getValue().getApplyId());
        assertEquals(2L, logCaptor.getValue().getUserId());
        assertEquals(VIRTUAL_ID, logCaptor.getValue().getVirtualId());
    }

    @Test
    @DisplayName("审批单过期后明文自动失效，回到脱敏视图")
    void expiredApprovalShouldFallbackToMasked() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        when(studentMapper.selectOne(any())).thenReturn(student());
        when(applyMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.profile(VIRTUAL_ID);
        assertEquals(true, result.get("masked"));
    }

    @Test
    @DisplayName("审核管理员持 privacy:view:direct 时可直接核验，并写留痕")
    void directViewShouldDecryptAndLog() {
        UserContext.set(new UserInfo(1L, "audit_admin", "AUDIT_ADMIN"));
        when(studentMapper.selectOne(any())).thenReturn(student());
        when(applyMapper.selectOne(any())).thenReturn(null);
        when(permissionHolder.hasPermission(eq("AUDIT_ADMIN"), any(String[].class))).thenReturn(true);
        when(privacyProfileMapper.selectOne(any())).thenReturn(profile());

        Map<String, Object> result = service.profile(VIRTUAL_ID);

        assertEquals(false, result.get("masked"));
        assertEquals(true, result.get("directView"));
        assertEquals("张三", result.get("name"));
        verify(accessLogMapper).insert(any());
    }

    @Test
    @DisplayName("maskedProfile 不判权、不解密，资助人联系电话也打码")
    void maskedProfileShouldMaskSponsorPhone() {
        SysUser sponsor = new SysUser();
        sponsor.setId(2L);
        sponsor.setRealName("张资助");
        sponsor.setPhone("13800000002");
        when(sysUserMapper.selectById(2L)).thenReturn(sponsor);

        Map<String, Object> result = service.maskedProfile("USER-2");

        assertEquals(true, result.get("masked"));
        assertEquals("张资助", result.get("realName"));
        assertNotNull(result.get("sponsorPhone"));
        assertFalse("13800000002".equals(result.get("sponsorPhone")));
    }

    @Test
    @DisplayName("未知虚拟ID的脱敏视图同样返回 404")
    void maskedProfileShouldRejectUnknownVirtualId() {
        when(studentMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.maskedProfile("STU-not-exist"));
        assertEquals(404, ex.getCode());
    }

    // ==================== 辅助 ====================

    private PrivacyProfile profile() {
        PrivacyProfile profile = new PrivacyProfile();
        profile.setId(1L);
        profile.setVirtualId(VIRTUAL_ID);
        profile.setNameEnc(CryptoUtils.encrypt("张三"));
        profile.setIdCardEnc(CryptoUtils.encrypt("110101200601011234"));
        profile.setPhoneEnc(CryptoUtils.encrypt("13812340002"));
        profile.setFamilyAddressEnc(CryptoUtils.encrypt("北京市海淀区中关村大街1号"));
        profile.setFamilySituationEnc(CryptoUtils.encrypt("低保家庭"));
        return profile;
    }

    @SuppressWarnings("unchecked")
    private UpdateWrapper<?> captureUpdateWrapper() {
        ArgumentCaptor<UpdateWrapper> captor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(applyMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }
}

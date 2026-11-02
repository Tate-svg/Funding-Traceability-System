package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.BusinessException;
import com.funding.common.MaskingGuard;
import com.funding.common.UserContext;
import com.funding.common.UserInfo;
import com.funding.entity.Fund;
import com.funding.mapper.FundMapper;
import com.funding.service.TimestampService;
import com.funding.vo.FundVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * 资金流水状态机单元测试（Mockito 模拟 Mapper，不访问数据库）
 * 覆盖：审核通过/驳回、重复审核、大小额拨付分支、字段级权限隔离
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FundServiceImplTest {

    @Mock
    private FundMapper fundMapper;

    @Mock
    private TimestampService timestampService;

    @Mock
    private MaskingGuard maskingGuard;

    @InjectMocks
    private FundServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "reviewThreshold", new BigDecimal("5000"));
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    private Fund fund(String status, String amount) {
        Fund fund = new Fund();
        fund.setId(1L);
        fund.setStudentId(1L);
        fund.setSponsorId(2L);
        fund.setAmount(new BigDecimal(amount));
        fund.setStatus(status);
        fund.setFundDate(LocalDate.now());
        return fund;
    }

    // ==================== 审核 ====================

    @Test
    @DisplayName("审核通过：状态置为 AUDITED 并生成唯一凭证号")
    void auditPassShouldSetAuditedAndVoucher() {
        UserContext.set(new UserInfo(1L, "audit_admin", "AUDIT_ADMIN"));
        when(fundMapper.selectById(1L)).thenReturn(fund(Fund.STATUS_SUBMITTED, "2000"));
        when(fundMapper.countVoucherOfDay(anyString())).thenReturn(0L);
        when(fundMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.audit(1L, "PASS", "资料齐全");

        UpdateWrapper<?> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Fund.STATUS_AUDITED));
        Object voucherNo = wrapper.getParamNameValuePairs().values().stream()
                .filter(v -> v instanceof String && ((String) v).startsWith("Voucher-"))
                .findFirst().orElse(null);
        assertNotNull(voucherNo);
        assertTrue(((String) voucherNo).matches("Voucher-\\d{8}-0001"));
    }

    @Test
    @DisplayName("审核驳回：状态置为 REJECTED 且不生成凭证")
    void auditRejectShouldSetRejected() {
        UserContext.set(new UserInfo(1L, "audit_admin", "AUDIT_ADMIN"));
        when(fundMapper.selectById(1L)).thenReturn(fund(Fund.STATUS_SUBMITTED, "2000"));
        when(fundMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.audit(1L, "REJECT", "材料不全");

        UpdateWrapper<?> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Fund.STATUS_REJECTED));
        assertTrue(wrapper.getParamNameValuePairs().values().stream()
                .noneMatch(v -> v instanceof String && ((String) v).startsWith("Voucher-")));
    }

    @Test
    @DisplayName("重复审核（状态已变化）被拒绝")
    void auditShouldRejectWhenStatusChanged() {
        UserContext.set(new UserInfo(1L, "audit_admin", "AUDIT_ADMIN"));
        when(fundMapper.selectById(1L)).thenReturn(fund(Fund.STATUS_SUBMITTED, "2000"));
        when(fundMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.audit(1L, "PASS", "重复审核"));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("单据不存在时返回 404")
    void auditShouldRejectMissingFund() {
        when(fundMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.audit(1L, "PASS", "ok"));
        assertEquals(404, ex.getCode());
    }

    // ==================== 拨付 ====================

    @Test
    @DisplayName("小额已审核单据可直接拨付")
    void disburseShouldAllowSmallAuditedFund() {
        UserContext.set(new UserInfo(3L, "disburse_admin", "DISBURSE_ADMIN"));
        when(fundMapper.selectById(1L)).thenReturn(fund(Fund.STATUS_AUDITED, "3000"));
        when(fundMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.disburse(1L);

        UpdateWrapper<?> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Fund.STATUS_DISBURSED));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(3L));
    }

    @Test
    @DisplayName("大额已审核单据未复核不得拨付")
    void disburseShouldRejectLargeAuditedFund() {
        UserContext.set(new UserInfo(3L, "disburse_admin", "DISBURSE_ADMIN"));
        when(fundMapper.selectById(1L)).thenReturn(fund(Fund.STATUS_AUDITED, "6000"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.disburse(1L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("复核"));
    }

    @Test
    @DisplayName("已复核的大额单据可拨付")
    void disburseShouldAllowReviewedLargeFund() {
        UserContext.set(new UserInfo(3L, "disburse_admin", "DISBURSE_ADMIN"));
        when(fundMapper.selectById(1L)).thenReturn(fund(Fund.STATUS_REVIEWED, "6000"));
        when(fundMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.disburse(1L);
        verify(fundMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("待审核单据不可拨付")
    void disburseShouldRejectSubmittedFund() {
        UserContext.set(new UserInfo(3L, "disburse_admin", "DISBURSE_ADMIN"));
        when(fundMapper.selectById(1L)).thenReturn(fund(Fund.STATUS_SUBMITTED, "1000"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.disburse(1L));
        assertEquals(400, ex.getCode());
    }

    // ==================== 查询权限隔离 ====================

    @Test
    @DisplayName("资助人查询按本人 sponsor_id 过滤")
    void pageShouldFilterBySponsorForSponsorRole() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        Page<FundVO> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        when(fundMapper.selectPageWithNames(any(), any(), any(), eq(2L), any())).thenReturn(page);

        service.page(1, 10, null, null, null);

        verify(fundMapper).selectPageWithNames(any(), isNull(), isNull(), eq(2L), isNull());
    }

    @Test
    @DisplayName("拨付管理员查看时用途与备注被服务端清空")
    void pageShouldMaskPurposeForDisburseAdmin() {
        UserContext.set(new UserInfo(3L, "disburse_admin", "DISBURSE_ADMIN"));
        FundVO vo = new FundVO();
        vo.setId(1L);
        vo.setPurpose("学费");
        vo.setRemark("春季");
        Page<FundVO> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>(java.util.Collections.singletonList(vo)));
        when(fundMapper.selectPageWithNames(any(), any(), any(), any(), any())).thenReturn(page);

        service.page(1, 10, null, null, null);

        assertNull(vo.getPurpose());
        assertNull(vo.getRemark());
    }

    @Test
    @DisplayName("资助人不能查看他人名下单据")
    void getByIdShouldRejectOtherSponsorFund() {
        UserContext.set(new UserInfo(2L, "sponsor", "SPONSOR"));
        FundVO vo = new FundVO();
        vo.setId(1L);
        vo.setSponsorId(9L);
        when(fundMapper.selectByIdWithNames(1L)).thenReturn(vo);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("单据不存在时详情返回 null")
    void getByIdShouldReturnNullWhenMissing() {
        when(fundMapper.selectByIdWithNames(99L)).thenReturn(null);
        assertNull(service.getById(99L));
        verify(fundMapper, never()).selectByIdWithNames(2L);
    }

    @SuppressWarnings("unchecked")
    private UpdateWrapper<?> captureUpdateWrapper() {
        ArgumentCaptor<UpdateWrapper> captor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(fundMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }
}

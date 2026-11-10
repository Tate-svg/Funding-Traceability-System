package com.funding.controller;

import com.funding.common.PermissionHolder;
import com.funding.common.BusinessException;
import com.funding.common.GlobalExceptionHandler;
import com.funding.common.UserContext;
import com.funding.common.UserInfo;
import com.funding.entity.PrivacyAccessApply;
import com.funding.mapper.PrivacyAccessApplyMapper;
import com.funding.service.AdminPrivacyConfigService;
import com.funding.service.PrivacyAccessService;
import com.funding.service.SensitiveViewSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 通过真实MVC路由验证会话归属、审批期限及防止伪造adminId。 */
@ExtendWith(MockitoExtension.class)
class PrivacyMaskControllerTest {
    @Mock private AdminPrivacyConfigService configService;
    @Mock private SensitiveViewSessionService sessionService;
    @Mock private PrivacyAccessApplyMapper applyMapper;
    @Mock private PrivacyAccessService privacyAccessService;
    @Mock private PermissionHolder permissionHolder;
    @InjectMocks private PrivacyMaskController controller;
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        UserInfo user = new UserInfo(); user.setUserId(1L); user.setRole("SPONSOR"); UserContext.set(user);
    }
    @AfterEach void clear() { UserContext.clear(); }
    private PrivacyAccessApply apply(String status, LocalDateTime expires) {
        PrivacyAccessApply value = new PrivacyAccessApply(); value.setId(9L); value.setApplicantId(1L);
        value.setVirtualId("STU-test"); value.setStatus(status); value.setExpireTime(expires); return value;
    }
    @Test void requestParameterCannotActAsAuthentication() throws Exception {
        UserContext.clear();
        mvc.perform(post("/api/privacy/mask/createViewSession").param("adminId", "1").param("targetId", "9"))
                .andExpect(jsonPath("$.code").value(401));
        verifyNoInteractions(sessionService, applyMapper);
    }
    @Test void expiredSessionCannotBeRenewed() throws Exception {
        when(applyMapper.selectById(9L)).thenReturn(apply("PENDING_L1", null));
        when(sessionService.touchSession("key", 1L, 9L)).thenReturn(null);
        mvc.perform(post("/api/privacy/mask/touchSession").param("sessionKey", "key").param("targetId", "9"))
                .andExpect(jsonPath("$.code").value(410));
    }
    @Test void expiredApprovalCannotBeExtendedByActivity() throws Exception {
        when(applyMapper.selectById(9L)).thenReturn(apply("APPROVED", LocalDateTime.now().minusMinutes(1)));
        mvc.perform(post("/api/privacy/mask/touchSession").param("sessionKey", "key").param("targetId", "9"))
                .andExpect(jsonPath("$.code").value(410));
        verifyNoInteractions(sessionService);
    }
    @Test void approvalDeadlineCapsClientDisplayDeadline() throws Exception {
        when(applyMapper.selectById(9L)).thenReturn(apply("APPROVED", LocalDateTime.now().plusSeconds(60)));
        when(sessionService.touchSession("key", 1L, 9L)).thenReturn(900L);
        when(sessionService.remainingSeconds("key", 1L, 9L)).thenReturn(900L);
        when(sessionService.absoluteSecondsRemaining("key", 1L, 9L)).thenReturn(7200L);
        mvc.perform(post("/api/privacy/mask/touchSession").param("sessionKey", "key").param("targetId", "9"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.remainingSeconds", lessThanOrEqualTo(60)))
                .andExpect(jsonPath("$.data.absoluteSecondsRemaining", lessThanOrEqualTo(60)));
    }
    @Test void readingChecksOwnerAndTargetWithoutRenewing() throws Exception {
        when(sessionService.checkSessionValid("key", 1L, 9L)).thenReturn(false);
        mvc.perform(get("/api/privacy/mask/getSensitiveDetail").param("sessionKey", "key").param("targetId", "9"))
                .andExpect(jsonPath("$.code").value(401));
        verify(sessionService).checkSessionValid("key", 1L, 9L);
        verify(sessionService, never()).touchSession(anyString(), anyLong(), anyLong());
        verifyNoInteractions(privacyAccessService);
    }
    @Test void closingCannotInvalidateSomeoneElsesSession() throws Exception {
        when(sessionService.closeSession("other-key", 1L)).thenReturn(false);
        mvc.perform(post("/api/privacy/mask/closeSession").param("sessionKey", "other-key"))
                .andExpect(jsonPath("$.code").value(403));
    }
    @Test void crossUserSessionIsForbiddenWithoutLoggingOutTheAccount() throws Exception {
        when(sessionService.checkSessionValid("other-key", 1L, 9L))
                .thenThrow(new BusinessException(403, "会话不属于当前用户或申请单"));
        mvc.perform(get("/api/privacy/mask/getSensitiveDetail").param("sessionKey", "other-key").param("targetId", "9"))
                .andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(privacyAccessService);
    }

}

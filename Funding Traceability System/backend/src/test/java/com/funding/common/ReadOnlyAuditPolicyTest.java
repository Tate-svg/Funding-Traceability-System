package com.funding.common;

import com.funding.controller.ReadOnlyAuditController;
import com.funding.controller.PrivacyAccessController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReadOnlyAuditPolicyTest {
    @AfterEach void clear() { UserContext.clear(); }
    @Test void explicitlyAllowsOnlyNecessaryReadEndpointsAndLogout() {
        for(String p:new String[]{"/api/auth/info","/api/audit-readonly/funds","/api/audit-readonly/funds/1","/api/audit-readonly/scores/2","/api/audit-readonly/timestamps","/api/audit-readonly/logs","/api/audit-readonly/verify"}) assertTrue(ReadOnlyAuditPolicy.allows("GET",p),p);
        assertTrue(ReadOnlyAuditPolicy.allows("POST","/api/auth/logout"));
    }
    @Test void deniesEveryBusinessWriteEvenIfPermissionWasAccidentallyGranted() throws Exception {
        RoleInterceptor interceptor=new RoleInterceptor(); PermissionHolder permissions=mock(PermissionHolder.class);
        ReflectionTestUtils.setField(interceptor,"permissionHolder",permissions);
        UserContext.set(new UserInfo(7L,"auditor","AUDITOR"));
        for(String method:new String[]{"POST","PUT","DELETE","PATCH"}) {
            MockHttpServletRequest req=new MockHttpServletRequest(method,"/api/audit-readonly/funds");
            MockHttpServletResponse res=new MockHttpServletResponse();
            assertFalse(interceptor.preHandle(req,res,new Object())); assertTrue(res.getContentAsString().contains("403"));
        }
        verifyNoInteractions(permissions);
    }
    @Test void deniesUnannotatedSensitiveReadsAndAlternateOldRoutes() throws Exception {
        RoleInterceptor interceptor=new RoleInterceptor(); UserContext.set(new UserInfo(7L,"auditor","AUDITOR"));
        HandlerMethod handler=new HandlerMethod(new PrivacyAccessController(),PrivacyAccessController.class.getMethod("profile",String.class));
        for(String path:new String[]{"/api/privacy/profile/STU-1","/api/privacy/mask/getSensitiveDetail","/api/students/1/attachments","/api/users/options","/api/funds/1","/api/operation-logs","/api/audit-readonly/../privacy/profile/1"}) {
            MockHttpServletResponse res=new MockHttpServletResponse();
            assertFalse(interceptor.preHandle(new MockHttpServletRequest("GET",path),res,handler),path);
        }
    }
    @Test void allowedReadsStillRequireTheDedicatedPermission() throws Exception {
        RoleInterceptor interceptor=new RoleInterceptor(); PermissionHolder permissions=mock(PermissionHolder.class);
        ReflectionTestUtils.setField(interceptor,"permissionHolder",permissions); UserContext.set(new UserInfo(7L,"auditor","AUDITOR"));
        HandlerMethod handler=new HandlerMethod(new ReadOnlyAuditController(),ReadOnlyAuditController.class.getMethod("funds",Integer.class,Integer.class));
        MockHttpServletRequest req=new MockHttpServletRequest("GET","/api/audit-readonly/funds");
        assertFalse(interceptor.preHandle(req,new MockHttpServletResponse(),handler));
        when(permissions.hasPermission(eq("AUDITOR"),any())).thenReturn(true);
        assertTrue(interceptor.preHandle(req,new MockHttpServletResponse(),handler));
    }
    @Test void supervisorKeepsExistingApprovalPermission() throws Exception {
        RoleInterceptor interceptor=new RoleInterceptor(); PermissionHolder permissions=mock(PermissionHolder.class);
        ReflectionTestUtils.setField(interceptor,"permissionHolder",permissions); UserContext.set(new UserInfo(6L,"supervisor","SUPERVISOR"));
        when(permissions.hasPermission(eq("SUPERVISOR"),any())).thenReturn(true);
        HandlerMethod handler=new HandlerMethod(new PrivacyAccessController(),PrivacyAccessController.class.getMethod("approve",Long.class,java.util.Map.class));
        assertTrue(interceptor.preHandle(new MockHttpServletRequest("POST","/api/privacy/access/1/approve"),new MockHttpServletResponse(),handler));
    }
    @Test void unsupportedMethodsHaveAControlledResponse() throws Exception {
        org.springframework.test.web.servlet.MockMvc mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new ReadOnlyAuditController()).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/audit-readonly/funds"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(405));
    }

}

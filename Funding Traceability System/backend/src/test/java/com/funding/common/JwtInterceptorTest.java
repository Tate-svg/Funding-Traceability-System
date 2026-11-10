package com.funding.common;

import com.funding.entity.SysUser;
import com.funding.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtInterceptorTest {
    private final JwtInterceptor interceptor=new JwtInterceptor();
    private final SysUserMapper users=mock(SysUserMapper.class);
    @BeforeEach void init() {
        ReflectionTestUtils.setField(interceptor,"userMapper",users);
        JwtUtils utils=new JwtUtils(); utils.setSecret("funding-test-only-very-long-key-for-unit-tests"); utils.setExpire(86400000L);
    }
    @AfterEach void clear() { UserContext.clear(); }
    private MockHttpServletRequest request() {
        MockHttpServletRequest req=new MockHttpServletRequest("POST","/api/funds");
        req.addHeader("Authorization","Bearer "+JwtUtils.generate(7L,"old-admin","AUDIT_ADMIN")); return req;
    }
    @Test void demotionTakesEffectForAnAlreadyIssuedToken() throws Exception {
        SysUser user=new SysUser();user.setId(7L);user.setUsername("auditor");user.setRole("AUDITOR");
        when(users.selectById(7L)).thenReturn(user);
        assertTrue(interceptor.preHandle(request(),new MockHttpServletResponse(),new Object()));
        assertEquals("AUDITOR",UserContext.getRole());
        assertFalse(new RoleInterceptor().preHandle(request(),new MockHttpServletResponse(),new Object()));
    }
    @Test void deletedAccountsCannotUseOldTokens() throws Exception {
        MockHttpServletResponse res=new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request(),res,new Object())); assertTrue(res.getContentAsString().contains("401"));
    }
}

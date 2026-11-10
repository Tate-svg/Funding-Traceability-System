package com.funding.common;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * JWT 认证拦截器
 * 校验 Authorization 头，白名单 /api/auth/login 放行
 * 解析成功后设置 UserContext，请求结束 clear
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @org.springframework.beans.factory.annotation.Autowired
    private com.funding.mapper.SysUserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // 登录接口白名单放行
        if (uri.endsWith("/api/auth/login")) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return false;
        }

        String token = auth.substring(7);
        try {
            Claims claims = JwtUtils.parse(token);
            Long userId = JwtUtils.extractUserId(claims);
            com.funding.entity.SysUser current = userMapper.selectById(userId);
            if (current == null) { writeUnauthorized(response); return false; }
            // 按数据库当前角色授权，降为只读或删除账号后旧JWT不能保留写权限。
            String username = current.getUsername();
            String role = current.getRole();
            UserContext.set(new UserInfo(userId, username, role));
            return true;
        } catch (Exception e) {
            log.warn("Token解析失败: {}", e.getMessage());
            writeUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write("{\"code\":401,\"message\":\"未登录或Token失效\",\"data\":null}");
            writer.flush();
        }
    }
}

package com.funding.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 角色权限拦截器
 * 检查处理方法或其所在类是否标注 @RequirePermission，
 * 若标注则要求当前用户角色拥有任一权限点，否则 403 拦截
 */
@Slf4j
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Autowired
    private PermissionHolder permissionHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 优先检查方法注解，再检查类注解
        RequirePermission require = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (require == null) {
            require = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (require == null) {
            return true;
        }
        // 需要权限点
        UserInfo user = UserContext.get();
        if (user == null || !permissionHolder.hasPermission(user.getRole(), require.value())) {
            log.warn("越权拦截: user={} role={} 所需权限={} uri={}",
                    user == null ? null : user.getUsername(),
                    user == null ? null : user.getRole(),
                    String.join(",", require.value()),
                    request.getRequestURI());
            writeForbidden(response);
            return false;
        }
        return true;
    }

    private void writeForbidden(HttpServletResponse response) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write("{\"code\":403,\"message\":\"权限不足\",\"data\":null}");
            writer.flush();
        }
    }
}

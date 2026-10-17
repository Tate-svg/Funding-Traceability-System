package com.funding.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注该方法（或类）需要的权限点，满足任一即放行
 * 由 RoleInterceptor 反射检查，配合 PermissionHolder 校验
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限点编码，如 fund:audit；满足任一放行
     */
    String[] value();
}

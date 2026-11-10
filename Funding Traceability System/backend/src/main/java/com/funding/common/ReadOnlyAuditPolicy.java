package com.funding.common;

/** 只读审计角色独立允许列表，覆盖没有权限注解的旧接口。 */
public final class ReadOnlyAuditPolicy {
    private ReadOnlyAuditPolicy() { }

    public static boolean allows(String method, String path) {
        if ("POST".equals(method) && "/api/auth/logout".equals(path)) return true;
        if (!"GET".equals(method)) return false;
        if ("/api/auth/info".equals(path)) return true;
        return path.matches("/api/audit-readonly/(funds(?:/[0-9]+)?|scores(?:/[0-9]+)?|timestamps|logs|verify)/?");
    }
}

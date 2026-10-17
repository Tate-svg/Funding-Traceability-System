package com.funding.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.funding.entity.RolePermission;
import com.funding.mapper.RolePermissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限点持有者：启动时全量加载 role_permission 表至内存
 * 提供 角色 -> 权限点集合 的校验能力
 */
@Slf4j
@Component
public class PermissionHolder {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    /** role -> permissions */
    private final Map<String, Set<String>> rolePermissions = new HashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    public synchronized void reload() {
        List<RolePermission> list = rolePermissionMapper.selectList(new QueryWrapper<>());
        Map<String, Set<String>> map = new HashMap<>();
        for (RolePermission rp : list) {
            map.computeIfAbsent(rp.getRole(), k -> new HashSet<>()).add(rp.getPermission());
        }
        rolePermissions.clear();
        rolePermissions.putAll(map);
        log.info("权限点加载完成: {} 个角色", map.size());
    }

    /**
     * 判断角色是否拥有任一权限点
     */
    public boolean hasPermission(String role, String[] points) {
        if (role == null || points == null || points.length == 0) {
            return false;
        }
        Set<String> owned = rolePermissions.get(role);
        if (owned == null) {
            return false;
        }
        for (String p : points) {
            if (owned.contains(p)) {
                return true;
            }
        }
        return false;
    }
}

package com.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-权限点映射
 */
@Data
@TableName("role_permission")
public class RolePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String role;

    private String permission;
}

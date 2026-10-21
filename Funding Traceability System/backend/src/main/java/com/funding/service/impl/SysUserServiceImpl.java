package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.SysUser;
import com.funding.mapper.SysUserMapper;
import com.funding.service.SysUserService;
import com.funding.vo.OptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统用户服务实现
 * 用户管理仅 AUDIT_ADMIN 可调用（Controller 已 @RequirePermission("user:manage")）
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<SysUser> page(Integer page, Integer size, String keyword) {
        Page<SysUser> p = new Page<>(page, size);
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("username", keyword)
                    .or().like("real_name", keyword)
                    .or().like("phone", keyword));
        }
        wrapper.orderByDesc("id");
        IPage<SysUser> result = sysUserMapper.selectPage(p, wrapper);
        // 列表不返回密码；联系方式（资助人电话）默认打码，明文须经隐私调取两级审批
        if (result.getRecords() != null) {
            result.getRecords().forEach(u -> {
                u.setPassword(null);
                u.setPhone(com.funding.util.MaskingEngine.maskOf("PHONE", u.getPhone()));
            });
        }
        return result;
    }

    @Override
    public void save(SysUser user) {
        user.setId(null);
        // Demo简化：明文存储密码
        sysUserMapper.insert(user);
    }

    @Override
    public void update(SysUser user) {
        // 若传入空密码则不更新密码字段（NOT_NULL 策略下 null 不会被更新）
        if (user.getPassword() != null && user.getPassword().isEmpty()) {
            user.setPassword(null);
        }
        // 列表返回的电话为打码值；含 * 说明未改动，不更新该字段防止打码值写回
        if (user.getPhone() != null && user.getPhone().contains("*")) {
            user.setPhone(null);
        }
        sysUserMapper.updateById(user);
    }

    @Override
    public void delete(Long id) {
        sysUserMapper.deleteById(id);
    }

    @Override
    public List<OptionVO> options(String role) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        if (role != null && !role.trim().isEmpty()) {
            wrapper.eq("role", role);
        }
        wrapper.orderByAsc("id");
        List<SysUser> users = sysUserMapper.selectList(wrapper);
        List<OptionVO> list = new ArrayList<>();
        for (SysUser u : users) {
            list.add(new OptionVO(u.getId(), u.getRealName()));
        }
        return list;
    }
}

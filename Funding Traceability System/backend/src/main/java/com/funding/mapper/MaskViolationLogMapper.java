package com.funding.mapper;

import com.funding.entity.MaskViolationLog;
import org.apache.ibatis.annotations.Insert;

/**
 * 脱敏拦截记录 Mapper
 * 硬性约定：不继承 BaseMapper，仅提供 insert，保证拦截留痕只读不可篡改
 */
public interface MaskViolationLogMapper {

    @Insert("INSERT INTO mask_violation_log (user_id, username, api, field, rule, hit_text, create_time) " +
            "VALUES (#{userId}, #{username}, #{api}, #{field}, #{rule}, #{hitText}, NOW())")
    int insert(MaskViolationLog log);
}

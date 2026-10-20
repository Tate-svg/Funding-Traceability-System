package com.funding.mapper;

import com.funding.entity.PrivacyAccessLog;
import org.apache.ibatis.annotations.Insert;

/**
 * 隐私明文调取留痕 Mapper
 * 硬性约定：不继承 BaseMapper，仅提供 insert，保证留痕只读不可篡改
 */
public interface PrivacyAccessLogMapper {

    @Insert("INSERT INTO privacy_access_log (apply_id, user_id, username, virtual_id, fields, create_time) " +
            "VALUES (#{applyId}, #{userId}, #{username}, #{virtualId}, #{fields}, NOW())")
    int insert(PrivacyAccessLog log);
}

package com.funding.privacymapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.funding.entity.PrivacyProfile;

/**
 * 学生隐私档案 Mapper（隐私库数据源，见 PrivacyDataSourceConfig）
 * 与业务库物理隔离，禁止跨库 JOIN
 */
public interface PrivacyProfileMapper extends BaseMapper<PrivacyProfile> {
}

package com.funding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.funding.entity.AdminPrivacyConfig;
import com.funding.mapper.AdminPrivacyConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminPrivacyConfigService extends ServiceImpl<AdminPrivacyConfigMapper, AdminPrivacyConfig> {

    /**
     * 根据管理员id获取隐私遮挡配置
     * @param adminId 管理员ID
     * @return 配置实体
     */
    public AdminPrivacyConfig getByAdminId(Long adminId){
        LambdaQueryWrapper<AdminPrivacyConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminPrivacyConfig::getAdminId, adminId);
        return this.getOne(wrapper);
    }

    /**
     * 获取住址二次遮挡开关，默认开启(1)
     */
    public Integer getHideFamilyAddress(Long adminId){
        AdminPrivacyConfig config = getByAdminId(adminId);
        if(config == null){
            return 1;
        }
        return config.getHideFamilyAddress();
    }

    /**
     * 获取银行卡二次遮挡开关，默认开启(1)
     */
    public Integer getHideBankCard(Long adminId){
        AdminPrivacyConfig config = getByAdminId(adminId);
        if(config == null){
            return 1;
        }
        return config.getHideBankCard();
    }

    /**
     * 更新管理员遮挡配置
     * @param adminId 管理员id
     * @param hideFamilyAddress 是否隐藏住址 1开启 0关闭
     * @param hideBankCard 是否隐藏银行卡 1开启 0关闭
     */
    @Transactional
    public void updateMaskConfig(Long adminId, Integer hideFamilyAddress, Integer hideBankCard){
        AdminPrivacyConfig config = getByAdminId(adminId);
        if(config == null){
            config = new AdminPrivacyConfig();
            config.setAdminId(adminId);
            config.setCreateTime(LocalDateTime.now());
        }
        config.setHideFamilyAddress(hideFamilyAddress);
        config.setHideBankCard(hideBankCard);
        config.setUpdateTime(LocalDateTime.now());
        this.saveOrUpdate(config);
    }

    public Boolean getMaskStatus(Long adminId) {
        AdminPrivacyConfig config = getByAdminId(adminId);
        if (config == null) {
            // 没有配置，默认关闭遮挡
            return false;
        }
        // 判断银行卡是否开启遮挡（1开启 / 0关闭）
        return config.getHideBankCard() != null && config.getHideBankCard() == 1;
    }

    public void switchMask(Long adminId, Boolean isSecondMask) {
        Integer val = isSecondMask ? 1 : 0;
        // 这里以银行卡遮挡为例，第二个参数是家庭地址，第三个银行卡
        updateMaskConfig(adminId, val, val);
    }
}
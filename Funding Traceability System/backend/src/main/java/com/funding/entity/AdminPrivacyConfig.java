package com.funding.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员隐私二次遮挡配置
 */
@Data
@TableName("admin_privacy_config")
public class AdminPrivacyConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 管理员id
     */
    private Long adminId;

    /**
     * 是否二次隐藏家庭住址详情；1开启，0关闭
     */
    private Integer hideFamilyAddress;

    /**
     * 是否二次隐藏银行卡；1开启，0关闭
     */
    private Integer hideBankCard;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
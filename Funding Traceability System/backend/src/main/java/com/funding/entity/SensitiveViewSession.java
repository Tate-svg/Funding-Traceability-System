package com.funding.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 敏感信息查看会话（超时自动关闭）
 */
@Data
@TableName("sensitive_view_session")
public class SensitiveViewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一key
     */
    private String sessionId;

    /**
     * 管理员id
     */
    private Long adminId;

    /**
     * 对应的privacy_access_apply申请单id
     */
    private Long targetProfileId;

    /**
     * 会话开始时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startTime;

    /**
     * 空闲期限；活动上报可续期，最长不超过创建后2小时（可配置）
     */
    private LocalDateTime expireTime;

    /**
     * 是否有效：1有效，0失效
     */
    private Integer isValid;
}
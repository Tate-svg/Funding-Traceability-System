package com.funding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.funding.entity.SensitiveViewSession;
import com.funding.mapper.SensitiveViewSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 敏感信息查看会话
 * 到期校验以数据库 expire_time 为准；Redis 仅作为可选的高速凭证（未部署 Redis 时自动退化为数据库校验）
 */
@Service
public class SensitiveViewSessionService extends ServiceImpl<SensitiveViewSessionMapper, SensitiveViewSession> {

    private static final String REDIS_PREFIX = "sensitive:view:";

    /** Redis 非必需依赖：环境未部署 Redis 时不应阻塞敏感的查看/关闭流程 */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${privacy.sensitive-session-expire:15}")
    private Integer expireMin;

    public Integer getExpireMin() {
        return expireMin;
    }

    /**
     * 创建敏感查看会话（有效期由 privacy.sensitive-session-expire 配置，默认15分钟）
     *
     * @param adminId  管理员id
     * @param targetId 调取申请单id
     * @return sessionKey
     */
    @Transactional
    public String createSession(Long adminId, Long targetId) {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        SensitiveViewSession session = new SensitiveViewSession();
        session.setSessionId(sessionId);
        session.setAdminId(adminId);
        session.setTargetProfileId(targetId);
        session.setStartTime(now);
        session.setExpireTime(now.plusMinutes(expireMin));
        session.setIsValid(1);
        this.save(session);

        setRedisKey(sessionId);
        return sessionId;
    }

    /**
     * 校验会话是否有效：数据库有效期 + 有效标记，并结合 Redis 凭证（如可用）
     */
    public boolean checkSessionValid(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        SensitiveViewSession session = this.getOne(new LambdaQueryWrapper<SensitiveViewSession>()
                .eq(SensitiveViewSession::getSessionId, sessionId)
                .last("LIMIT 1"));
        if (session == null || session.getIsValid() == null || session.getIsValid() != 1) {
            return false;
        }
        LocalDateTime expireTime = session.getExpireTime();
        if (expireTime != null && !expireTime.isAfter(LocalDateTime.now())) {
            markInvalid(sessionId);
            deleteRedisKey(sessionId);
            return false;
        }
        // Redis 可用时以其凭证是否存续为准（手动关闭/到期均会删除）
        Boolean hasKey = hasRedisKey(sessionId);
        if (Boolean.FALSE.equals(hasKey)) {
            markInvalid(sessionId);
            return false;
        }
        return true;
    }

    /**
     * 手动关闭会话
     */
    @Transactional
    public void closeSession(String sessionId) {
        deleteRedisKey(sessionId);
        markInvalid(sessionId);
    }

    private void markInvalid(String sessionId) {
        LambdaUpdateWrapper<SensitiveViewSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SensitiveViewSession::getSessionId, sessionId)
                .set(SensitiveViewSession::getIsValid, 0);
        this.update(wrapper);
    }

    private void setRedisKey(String sessionId) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(REDIS_PREFIX + sessionId, "1", expireMin, TimeUnit.MINUTES);
            }
        } catch (Exception ignore) {
            // Redis 不可用时忽略，到期判定由数据库承担
        }
    }

    private Boolean hasRedisKey(String sessionId) {
        try {
            if (redisTemplate == null) {
                return null;
            }
            return redisTemplate.hasKey(REDIS_PREFIX + sessionId);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void deleteRedisKey(String sessionId) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(REDIS_PREFIX + sessionId);
            }
        } catch (Exception ignore) {
            // 忽略 Redis 异常
        }
    }
}

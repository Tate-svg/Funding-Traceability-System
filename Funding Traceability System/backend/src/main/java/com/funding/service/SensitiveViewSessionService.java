package com.funding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.funding.entity.SensitiveViewSession;
import com.funding.common.BusinessException;
import com.funding.mapper.SensitiveViewSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 敏感查看会话：默认15分钟无操作过期，最长2小时。
 * 读取本身不续期；只有当前用户对当前申请单上报操作才可续期。
 * 数据库为授权依据，Redis是可选缓存，缓存丢失不能覆盖数据库的会话状态。
 */
@Service
public class SensitiveViewSessionService extends ServiceImpl<SensitiveViewSessionMapper, SensitiveViewSession> {
    private static final String REDIS_PREFIX = "sensitive:view:";

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${privacy.sensitive-session-expire:15}")
    private Integer expireMin = 15;

    @Value("${privacy.sensitive-session-max:120}")
    private Integer maxSessionMin = 120;

    private Clock clock = Clock.systemDefaultZone();

    public Integer getExpireMin() { return expireMin; }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    @Transactional
    public String createSession(Long adminId, Long targetId) {
        if (adminId == null || targetId == null) {
            throw new IllegalArgumentException("会话必须绑定用户和申请单");
        }
        LocalDateTime now = now();
        SensitiveViewSession session = new SensitiveViewSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setAdminId(adminId);
        session.setTargetProfileId(targetId);
        session.setStartTime(now);
        session.setExpireTime(now.plusMinutes(Math.min(expireMin, maxSessionMin)));
        session.setIsValid(1);
        this.save(session);
        cache(session.getSessionId(), Duration.between(now, session.getExpireTime()).getSeconds());
        return session.getSessionId();
    }

    public boolean checkSessionValid(String sessionId, Long adminId, Long targetId) {
        return remainingSeconds(sessionId, adminId, targetId) > 0;
    }

    public long remainingSeconds(String sessionId, Long adminId, Long targetId) {
        SensitiveViewSession session = validSession(sessionId, adminId, targetId);
        return session == null ? 0 : Duration.between(now(), deadline(session)).getSeconds();
    }

    public long absoluteSecondsRemaining(String sessionId, Long adminId, Long targetId) {
        SensitiveViewSession session = validSession(sessionId, adminId, targetId);
        return session == null ? 0
                : Duration.between(now(), session.getStartTime().plusMinutes(maxSessionMin)).getSeconds();
    }

    /** 返回续期后的剩余秒数；无权、关闭或超时的会话不可复活。 */
    @Transactional
    public Long touchSession(String sessionId, Long adminId, Long targetId) {
        SensitiveViewSession session = validSession(sessionId, adminId, targetId);
        if (session == null) return null;
        LocalDateTime now = now();
        LocalDateTime next = now.plusMinutes(expireMin);
        LocalDateTime max = session.getStartTime().plusMinutes(maxSessionMin);
        if (next.isAfter(max)) next = max;
        // 条件更新防止并发关闭/过期与续期之间的竞态。
        LambdaUpdateWrapper<SensitiveViewSession> update = new LambdaUpdateWrapper<>();
        update.eq(SensitiveViewSession::getSessionId, sessionId)
                .eq(SensitiveViewSession::getAdminId, adminId)
                .eq(SensitiveViewSession::getTargetProfileId, targetId)
                .eq(SensitiveViewSession::getIsValid, 1)
                .gt(SensitiveViewSession::getExpireTime, now)
                .set(SensitiveViewSession::getExpireTime, next);
        if (!this.update(update)) return null;
        long seconds = Duration.between(now, next).getSeconds();
        cache(sessionId, seconds);
        return seconds;
    }

    /** 幂等关闭本人会话；不能使用他人的key关闭其会话。 */
    @Transactional
    public boolean closeSession(String sessionId, Long adminId) {
        if (sessionId == null || sessionId.trim().isEmpty() || adminId == null) return false;
        SensitiveViewSession session = this.getOne(new LambdaQueryWrapper<SensitiveViewSession>()
                .eq(SensitiveViewSession::getSessionId, sessionId).last("LIMIT 1"));
        if (session == null || !adminId.equals(session.getAdminId())) return false;
        invalidate(sessionId);
        return true;
    }

    private SensitiveViewSession validSession(String sessionId, Long adminId, Long targetId) {
        if (sessionId == null || sessionId.trim().isEmpty() || adminId == null || targetId == null) return null;
        SensitiveViewSession session = this.getOne(new LambdaQueryWrapper<SensitiveViewSession>()
                .eq(SensitiveViewSession::getSessionId, sessionId).last("LIMIT 1"));
        // 先检查归属，别人的错误请求不应让合法用户的会话失效。
        if (session == null) return null;
        if (!adminId.equals(session.getAdminId()) || !targetId.equals(session.getTargetProfileId())) {
            throw new BusinessException(403, "会话不属于当前用户或申请单");
        }
        if (!Integer.valueOf(1).equals(session.getIsValid())) return null;
        if (session.getExpireTime() == null || session.getStartTime() == null
                || !deadline(session).isAfter(now())) {
            invalidate(sessionId);
            return null;
        }
        return session;
    }

    private LocalDateTime deadline(SensitiveViewSession session) {
        LocalDateTime max = session.getStartTime().plusMinutes(maxSessionMin);
        return session.getExpireTime().isBefore(max) ? session.getExpireTime() : max;
    }

    private void invalidate(String sessionId) {
        this.update(new LambdaUpdateWrapper<SensitiveViewSession>()
                .eq(SensitiveViewSession::getSessionId, sessionId)
                .set(SensitiveViewSession::getIsValid, 0));
        try {
            if (redisTemplate != null) redisTemplate.delete(REDIS_PREFIX + sessionId);
        } catch (Exception ignored) { /* 数据库仍为失效依据 */ }
    }

    private void cache(String sessionId, long seconds) {
        try {
            if (redisTemplate != null && seconds > 0) {
                redisTemplate.opsForValue().set(REDIS_PREFIX + sessionId, "1", seconds, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) { /* Redis未部署不影响数据库授权判断 */ }
    }
}

package com.funding.service;

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

@Service
public class SensitiveViewSessionService extends ServiceImpl<SensitiveViewSessionMapper, SensitiveViewSession> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${privacy.sensitive-session-expire:15}")
    private Integer expireMin;

    /**
     * 创建敏感查看会话
     * @param adminId 管理员id
     * @param targetProfileId 隐私资料id
     * @return sessionId
     */
    @Transactional
    public String createViewSession(Long adminId, Long targetProfileId){
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(expireMin);

        // 存入redis
        String redisKey = "sensitive:view:" + sessionId;
        redisTemplate.opsForValue().set(redisKey, true, expireMin, TimeUnit.MINUTES);

        // 存入数据库
        SensitiveViewSession session = new SensitiveViewSession();
        session.setSessionId(sessionId);
        session.setAdminId(adminId);
        session.setTargetProfileId(targetProfileId);
        session.setStartTime(now);
        session.setExpireTime(expireTime);
        session.setIsValid(1);
        this.save(session);
        return sessionId;
    }

    /**
     * 校验会话是否有效
     * @param sessionId 会话id
     * @return true有效 / false失效
     */
    public boolean checkSessionValid(String sessionId){
        String redisKey = "sensitive:view:" + sessionId;
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if(Boolean.FALSE.equals(hasKey)){
            // 标记数据库会话失效
            LambdaUpdateWrapper<SensitiveViewSession> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(SensitiveViewSession::getSessionId, sessionId)
                    .set(SensitiveViewSession::getIsValid, 0);
            this.update(wrapper);
            return false;
        }
        return true;
    }

    /**
     * 手动关闭会话
     * @param sessionId 会话id
     */
    @Transactional
    public void closeSession(String sessionId){
        String redisKey = "sensitive:view:" + sessionId;
        redisTemplate.delete(redisKey);
        LambdaUpdateWrapper<SensitiveViewSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SensitiveViewSession::getSessionId, sessionId)
                .set(SensitiveViewSession::getIsValid, 0);
        this.update(wrapper);
    }

    public String createSession(Long adminId, Long targetId) {
        // 生成唯一sessionId，这里用UUID
        String sessionId = java.util.UUID.randomUUID().toString();

        // 存入数据库
        SensitiveViewSession session = new SensitiveViewSession();
        session.setSessionId(sessionId);
        session.setAdminId(adminId);
        session.setTargetId(targetId);
        session.setIsValid(1);
        this.save(session);

        // 写入redis
        String redisKey = "sensitive:view:" + sessionId;
        redisTemplate.opsForValue().set(redisKey, "1");

        return sessionId;
    }
}
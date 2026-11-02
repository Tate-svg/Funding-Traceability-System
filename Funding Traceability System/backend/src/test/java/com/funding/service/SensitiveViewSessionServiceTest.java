package com.funding.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.funding.entity.SensitiveViewSession;
import com.funding.mapper.SensitiveViewSessionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 敏感查看会话单元测试：创建、有效期校验、过期自动失效、手动关闭
 * Redis 未部署时（redisTemplate 为 null）应自动退化为数据库到期校验
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensitiveViewSessionServiceTest {

    @Mock
    private SensitiveViewSessionMapper mapper;

    @InjectMocks
    private SensitiveViewSessionService service;

    /**
     * 纯 Mock 环境没有 Spring 容器，需手动初始化实体的 TableInfo，
     * 否则 LambdaQueryWrapper / LambdaUpdateWrapper 取不到字段映射缓存
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SensitiveViewSession.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expireMin", 15);
    }

    private SensitiveViewSession session(String id, LocalDateTime expire, int valid) {
        SensitiveViewSession session = new SensitiveViewSession();
        session.setId(1L);
        session.setSessionId(id);
        session.setAdminId(1L);
        session.setTargetProfileId(1L);
        session.setStartTime(LocalDateTime.now());
        session.setExpireTime(expire);
        session.setIsValid(valid);
        return session;
    }

    @Test
    @DisplayName("创建会话：落库并设置默认15分钟有效期")
    void createSessionShouldPersistWithExpireMinutes() {
        when(mapper.insert(any())).thenReturn(1);

        String key = service.createSession(1L, 2L);

        assertNotNull(key);
        ArgumentCaptor<SensitiveViewSession> captor =
                ArgumentCaptor.forClass(SensitiveViewSession.class);
        verify(mapper).insert(captor.capture());
        SensitiveViewSession saved = captor.getValue();
        assertEquals(key, saved.getSessionId());
        assertEquals(1L, saved.getAdminId());
        assertEquals(2L, saved.getTargetProfileId());
        assertEquals(1, saved.getIsValid());
        long minutes = Duration.between(saved.getStartTime(), saved.getExpireTime()).toMinutes();
        assertEquals(15, minutes);
    }

    @Test
    @DisplayName("未过期且有效的会话校验通过")
    void validSessionShouldPass() {
        when(mapper.selectOne(any())).thenReturn(
                session("key-1", LocalDateTime.now().plusMinutes(10), 1));

        assertTrue(service.checkSessionValid("key-1"));
    }

    @Test
    @DisplayName("已过期会话校验失败，并被标记失效")
    void expiredSessionShouldFailAndBeInvalidated() {
        when(mapper.selectOne(any())).thenReturn(
                session("key-2", LocalDateTime.now().minusMinutes(1), 1));

        assertFalse(service.checkSessionValid("key-2"));
        verify(mapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    @DisplayName("已手动关闭的会话校验失败")
    void closedSessionShouldFail() {
        when(mapper.selectOne(any())).thenReturn(
                session("key-3", LocalDateTime.now().plusMinutes(10), 0));

        assertFalse(service.checkSessionValid("key-3"));
    }

    @Test
    @DisplayName("空会话凭证直接失败，不查库")
    void blankSessionShouldFailWithoutQuery() {
        assertFalse(service.checkSessionValid(null));
        assertFalse(service.checkSessionValid(""));
        verify(mapper, org.mockito.Mockito.never()).selectOne(any());
    }

    @Test
    @DisplayName("关闭会话时更新为失效")
    void closeSessionShouldMarkInvalid() {
        service.closeSession("key-4");

        verify(mapper).update(isNull(), any(Wrapper.class));
    }
}

package com.funding.common;

import com.funding.entity.OperationLogEntity;
import com.funding.mapper.OperationLogMapper;
import com.funding.service.TimestampService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 操作日志切面
 * 业务执行成功后同步写入只读日志（不用 @Async，避免 ThreadLocal 丢失）；
 * 同时为该业务节点加盖系统可信时间戳（本地模拟 TSA，前端不可传入）；
 * 日志/时间戳写入失败仅 warn，不影响主业务
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private TimestampService timestampService;

    @Around("@annotation(com.funding.common.OperationLog)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // 先执行业务，成功后才记录日志
        Object result = pjp.proceed();
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            OperationLog ann = method.getAnnotation(OperationLog.class);
            Long targetId = extractTargetId(pjp.getArgs());

            OperationLogEntity entity = new OperationLogEntity();
            UserInfo user = UserContext.get();
            if (user != null) {
                entity.setUserId(user.getUserId());
                entity.setUsername(user.getUsername());
                entity.setRole(user.getRole());
            }
            entity.setOperation(ann.value());
            entity.setTargetType(ann.type());
            entity.setTargetId(targetId);
            entity.setContent(truncate(buildContent(ann, pjp.getArgs())));
            operationLogMapper.insert(entity);

            // 可信时间戳盖章：全局序列号 + 哈希链固化时序
            timestampService.stamp(ann.value(), ann.type(), targetId, pjp.getArgs());
        } catch (Exception e) {
            log.warn("操作日志/时间戳写入失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 目标ID提取：优先取 Long 类型参数（/{id} 路径变量）；
     * 兜底反射取实体参数的 getId()（新增场景 insert 后 MyBatis-Plus 已回填主键）
     */
    private Long extractTargetId(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        for (Object arg : args) {
            if (arg == null || arg instanceof Long || arg instanceof Number || arg instanceof String
                    || arg instanceof Map || arg instanceof javax.servlet.http.HttpServletResponse
                    || arg instanceof javax.servlet.http.HttpServletRequest) {
                continue;
            }
            try {
                Object id = arg.getClass().getMethod("getId").invoke(arg);
                if (id instanceof Long) {
                    return (Long) id;
                }
            } catch (Exception ignored) {
                // 非实体参数，忽略
            }
        }
        return null;
    }

    private String buildContent(OperationLog ann, Object[] args) {
        StringBuilder sb = new StringBuilder(ann.value());
        if (args != null) {
            for (Object arg : args) {
                // 跳过 HttpServletResponse 等不可序列化参数；学生隐私入参不落日志明文
                if (arg == null || arg instanceof javax.servlet.http.HttpServletResponse
                        || arg instanceof javax.servlet.http.HttpServletRequest) {
                    continue;
                }
                if (arg instanceof com.funding.vo.StudentSaveRequest) {
                    sb.append(" | [隐私入参已省略]");
                    continue;
                }
                sb.append(" | ").append(formatArg(arg));
            }
        }
        return sb.toString();
    }

    /**
     * 参数可读化：MultipartFile 取文件名与大小，Map 拼 key=value，超长字符串截断
     */
    private String formatArg(Object arg) {
        if (arg instanceof org.springframework.web.multipart.MultipartFile) {
            org.springframework.web.multipart.MultipartFile f = (org.springframework.web.multipart.MultipartFile) arg;
            return "文件[" + f.getOriginalFilename() + ", " + (f.getSize() / 1024) + "KB]";
        }
        if (arg instanceof Map) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) arg).entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(e.getKey()).append("=").append(truncate(String.valueOf(e.getValue()), 60));
                first = false;
            }
            return sb.append("]").toString();
        }
        if (arg instanceof String) {
            return truncate((String) arg, 60);
        }
        return String.valueOf(arg);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}

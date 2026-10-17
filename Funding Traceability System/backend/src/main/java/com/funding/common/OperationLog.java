package com.funding.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注需要记录只读日志的接口
 * 由 OperationLogAspect 在业务执行成功后写入 operation_log 表
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 操作描述，如"审核资助申请"
     */
    String value();

    /**
     * 目标类型，如 fund / review_task / student / sys_user
     */
    String type() default "";
}

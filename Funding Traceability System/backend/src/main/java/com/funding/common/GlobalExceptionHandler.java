package com.funding.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(org.springframework.dao.DuplicateKeyException e) {
        log.warn("唯一键冲突: {}", e.getMessage());
        return Result.error(400, "数据冲突，请重试");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器错误", e);
        return Result.error(500, "服务器错误: " + e.getMessage());
    }
}

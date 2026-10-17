package com.funding.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局序列化配置：LocalDateTime 统一输出 yyyy-MM-dd HH:mm:ss，
 * 避免前端时间字段出现 ISO 原始格式（2026-10-17T19:16:42）。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return builder -> builder.serializers(new LocalDateTimeSerializer(formatter));
    }
}

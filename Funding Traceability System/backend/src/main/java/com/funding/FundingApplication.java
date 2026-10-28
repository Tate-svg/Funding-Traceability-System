package com.funding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 助学帮扶管理系统 启动类
 * Mapper 扫描见双数据源配置：BusinessDataSourceConfig(com.funding.mapper) /
 * PrivacyDataSourceConfig(com.funding.privacymapper)
 */
@SpringBootApplication
@EnableScheduling   //开启定时任务
@EnableAsync        //开启异步
public class FundingApplication {
    public static void main(String[] args) {
        SpringApplication.run(FundingApplication.class, args);
    }
}
